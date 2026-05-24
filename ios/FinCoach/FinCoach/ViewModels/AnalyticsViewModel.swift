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
    @Published var period: AnalyticsPeriod = .month
    @Published var excludedChartCategories: Set<String> = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var showError = false

    private let transactionsService = TransactionsService()
    private let categoriesService = CategoriesService()
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

    func toggleChartCategory(_ category: String) {
        if excludedChartCategories.contains(category) {
            excludedChartCategories.remove(category)
        } else {
            excludedChartCategories.insert(category)
        }
    }

    var filteredCategoryBreakdown: [CategorySpending] {
        let all = categoryBreakdown
        guard !excludedChartCategories.isEmpty else { return all }
        let visible = all.filter { !excludedChartCategories.contains($0.category) }
        let visibleTotal = visible.reduce(0) { $0 + $1.amount }
        guard visibleTotal > 0 else { return [] }
        return visible.map { item in
            CategorySpending(
                category: item.category,
                icon: item.icon,
                amount: item.amount,
                percentage: item.amount / visibleTotal * 100,
                color: item.color
            )
        }
    }

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
        do {
            let (t, c) = try await (txns, cats)
            transactions = t
            categories   = c
        } catch {
            presentError(error.localizedDescription)
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
        Color(red: 0.35, green: 0.47, blue: 0.88),
        Color(red: 0.94, green: 0.38, blue: 0.31),
        Color(red: 0.96, green: 0.72, blue: 0.25),
        Color(red: 0.87, green: 0.36, blue: 0.62),
        Color(red: 0.52, green: 0.38, blue: 0.82),
        Color(red: 0.25, green: 0.72, blue: 0.86),
        Color(red: 0.93, green: 0.57, blue: 0.22),
        Color(red: 0.85, green: 0.33, blue: 0.43),
        Color(red: 0.60, green: 0.45, blue: 0.90),
        Color(red: 0.30, green: 0.62, blue: 0.92),
        Color(red: 0.42, green: 0.82, blue: 0.72),
        Color(red: 0.95, green: 0.60, blue: 0.40),
    ]
}
