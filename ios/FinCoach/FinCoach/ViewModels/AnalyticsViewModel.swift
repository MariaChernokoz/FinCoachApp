//
//  AnalyticsViewModel.swift
//  FinCoach
//

import Foundation
import SwiftUI
import Combine

enum AnalyticsPeriod: String, CaseIterable, Identifiable {
    case week        = "Неделя"
    case month       = "Месяц"
    case threeMonths = "3 мес."
    case year        = "Год"
    var id: String { rawValue }

    var dateRange: (start: Date, end: Date) {
        let cal = Calendar.current
        let now = Date()
        let end = cal.date(bySettingHour: 23, minute: 59, second: 59, of: now) ?? now
        let start: Date
        switch self {
        case .week:        start = cal.date(byAdding: .day,   value: -6,  to: cal.startOfDay(for: now))!
        case .month:       start = cal.date(byAdding: .month, value: -1,  to: cal.startOfDay(for: now))!
        case .threeMonths: start = cal.date(byAdding: .month, value: -3,  to: cal.startOfDay(for: now))!
        case .year:        start = cal.date(byAdding: .year,  value: -1,  to: cal.startOfDay(for: now))!
        }
        return (start, end)
    }
}

struct CategorySpending: Identifiable {
    let id = UUID()
    let category: String
    let icon: String
    let amount: Double
    let percentage: Double
    let color: Color
}

struct MonthlyPoint: Identifiable {
    let id = UUID()
    let month: Date
    let label: String
    let income: Double
    let expense: Double
}

@MainActor
final class AnalyticsViewModel: ObservableObject {
    @Published private(set) var transactions: [Transaction] = []
    @Published private(set) var categories: [Category] = []
    @Published private(set) var budgets: [Budget] = []
    @Published var period: AnalyticsPeriod = .month
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var showError = false

    private let transactionsService = TransactionsService()
    private let categoriesService = CategoriesService()
    private let budgetService = BudgetService()
    private var userId: String?

    // MARK: - Period aggregates

    var periodTransactions: [Transaction] {
        let (start, end) = period.dateRange
        return transactions.filter { $0.date >= start && $0.date <= end }
    }

    var periodIncome: Double {
        periodTransactions.filter(\.isIncome).reduce(0) { $0 + $1.amount }
    }

    var periodExpense: Double {
        periodTransactions.filter { !$0.isIncome }.reduce(0) { $0 + $1.amount }
    }

    var periodSavings: Double { periodIncome - periodExpense }

    var periodDays: Int {
        let (start, end) = period.dateRange
        return max(1, Calendar.current.dateComponents([.day], from: start, to: end).day ?? 1)
    }

    var averageDailyExpense: Double {
        periodExpense / Double(periodDays)
    }

    var savingsRate: Double? {
        guard periodIncome > 0 else { return nil }
        return (periodIncome - periodExpense) / periodIncome * 100
    }

    // MARK: - Donut chart

    var categoryBreakdown: [CategorySpending] {
        let expenses = periodTransactions.filter { !$0.isIncome }
        let total = expenses.reduce(0) { $0 + $1.amount }
        guard total > 0 else { return [] }

        let grouped = Dictionary(grouping: expenses) { resolvedTitle(for: $0) }
        let sorted = grouped
            .map { title, txns -> (title: String, icon: String, amount: Double) in
                let amount = txns.reduce(0) { $0 + $1.amount }
                let icon = resolvedIcon(for: txns.first)
                return (title, icon, amount)
            }
            .sorted { $0.amount > $1.amount }

        return sorted.enumerated().map { index, item in
            CategorySpending(
                category: item.title,
                icon: item.icon,
                amount: item.amount,
                percentage: item.amount / total * 100,
                color: Self.palette[index % Self.palette.count]
            )
        }
    }

    // MARK: - Monthly bar chart (last 6 months)

    var monthlyPoints: [MonthlyPoint] {
        let cal = Calendar.current
        let now = Date()
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "ru_RU")
        formatter.dateFormat = "MMM"

        return (0..<6).reversed().compactMap { offset -> MonthlyPoint? in
            guard
                let anchor    = cal.date(byAdding: .month, value: -offset, to: now),
                let monthStart = cal.date(from: cal.dateComponents([.year, .month], from: anchor)),
                let nextMonth  = cal.date(byAdding: .month, value: 1, to: monthStart),
                let monthEnd   = cal.date(byAdding: .second, value: -1, to: nextMonth)
            else { return nil }

            let txns    = transactions.filter { $0.date >= monthStart && $0.date <= monthEnd }
            let income  = txns.filter(\.isIncome).reduce(0) { $0 + $1.amount }
            let expense = txns.filter { !$0.isIncome }.reduce(0) { $0 + $1.amount }
            let label   = formatter.string(from: monthStart).capitalized
            return MonthlyPoint(month: monthStart, label: label, income: income, expense: expense)
        }
    }

    // MARK: - Budget helpers

    func category(for budget: Budget) -> Category? {
        categories.first { $0.id == budget.categoryId || $0.title == budget.categoryId }
    }

    func currentMonthSpent(for budget: Budget) -> Double {
        let cal = Calendar.current
        let now = Date()
        let cat = category(for: budget)
        return transactions
            .filter { !$0.isIncome && cal.isDate($0.date, equalTo: now, toGranularity: .month) }
            .filter { transaction in
                if let cat {
                    return transaction.category == cat.id || transaction.category == cat.title
                }
                return transaction.category == budget.categoryId
            }
            .reduce(0) { $0 + $1.amount }
    }

    func spentFraction(for budget: Budget) -> Double {
        guard budget.limitAmount > 0 else { return 0 }
        return min(currentMonthSpent(for: budget) / budget.limitAmount, 1.0)
    }

    // MARK: - Lifecycle

    func start(userId: String) {
        guard self.userId != userId else { return }
        self.userId = userId
        Task { await load() }
    }

    func stop() { userId = nil }

    func load() async {
        guard let userId else { return }
        isLoading = true
        defer { isLoading = false }
        async let txns = transactionsService.fetchTransactions(userId: userId)
        async let cats = categoriesService.fetchCategories()
        async let bdgs = budgetService.fetchBudgets(userId: userId)
        do {
            let (t, c, b) = try await (txns, cats, bdgs)
            transactions = t
            categories   = c
            budgets      = b
        } catch {
            presentError(error.localizedDescription)
        }
    }

    // MARK: - Budget CRUD

    func saveBudget(existing: Budget?, categoryId: String, limitAmount: Double) async {
        guard let userId else { return }
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        let cal = Calendar.current
        let periodStart = Int64(
            (cal.date(from: cal.dateComponents([.year, .month], from: Date())) ?? Date())
                .timeIntervalSince1970 * 1000
        )
        let budget = Budget(
            id: existing?.id ?? UUID().uuidString,
            userId: userId,
            categoryId: categoryId,
            limitAmount: limitAmount,
            currentSpent: existing?.currentSpent ?? 0,
            period: "month",
            periodStart: existing?.periodStart ?? periodStart,
            updatedAt: now
        )
        do {
            if existing == nil {
                try await budgetService.create(budget)
                budgets.append(budget)
            } else {
                try await budgetService.update(budget)
                if let i = budgets.firstIndex(where: { $0.id == budget.id }) {
                    budgets[i] = budget
                }
            }
        } catch {
            presentError(error.localizedDescription)
        }
    }

    func deleteBudget(_ budget: Budget) {
        budgets.removeAll { $0.id == budget.id }
        Task {
            do {
                try await budgetService.delete(budget)
            } catch {
                presentError(error.localizedDescription)
                await load()
            }
        }
    }

    // MARK: - Private helpers

    private func resolvedTitle(for transaction: Transaction) -> String {
        categories
            .first { $0.id == transaction.category || $0.title == transaction.category }?
            .title ?? (transaction.category.isEmpty ? "Без категории" : transaction.category)
    }

    private func resolvedIcon(for transaction: Transaction?) -> String {
        guard let transaction else { return "tag" }
        return categories
            .first { $0.id == transaction.category || $0.title == transaction.category }?
            .icon ?? "tag"
    }

    private func presentError(_ message: String) {
        errorMessage = message
        showError = true
    }

    static let palette: [Color] = [
        AppColors.lightGreenFrameColor,
        AppColors.purpleFrameColor,
        .blue,
        .orange,
        .red,
        .cyan,
        .indigo,
        .pink,
        .brown,
        .teal,
        .mint,
        Color(.systemYellow)
    ]
}
