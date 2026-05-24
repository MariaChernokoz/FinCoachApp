//
//  GoalsViewModel.swift
//  FinCoach
//

import Foundation
import SwiftUI
import Combine

struct GoalInsight {
    let message: String
    let icon: String
    let accentColor: Color
}

@MainActor
final class GoalsViewModel: ObservableObject {
    @Published private(set) var goals: [Goal] = []
    @Published private(set) var budgets: [Budget] = []
    @Published private(set) var categories: [Category] = []
    @Published private(set) var transactions: [Transaction] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var showError = false

    private let goalService = GoalService()
    private let budgetService = BudgetService()
    private let categoriesService = CategoriesService()
    private let transactionsService = TransactionsService()
    private var userId: String?

    // MARK: - Lifecycle

    init() {
        #if DEBUG
        if ProcessInfo.processInfo.environment["XCODE_RUNNING_FOR_PREVIEWS"] == "1" {
            let createdAt = Int64((Date().timeIntervalSince1970 - 90 * 24 * 3600) * 1000)
            goals = [
                Goal(id: "1", userId: "preview", title: "Отпуск в Сочи",
                     targetAmount: 120_000, savedAmount: 45_000, createdAt: createdAt),
                Goal(id: "2", userId: "preview", title: "Новый ноутбук",
                     targetAmount: 80_000, savedAmount: 12_000, createdAt: createdAt)
            ]
        }
        #endif
    }

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
        async let gs = goalService.fetchGoals(userId: userId)
        async let bs = budgetService.fetchBudgets(userId: userId)
        async let cs = categoriesService.fetchCategories()
        async let ts = transactionsService.fetchTransactions(userId: userId)
        do {
            let (g, b, c, t) = try await (gs, bs, cs, ts)
            goals = g
            budgets = b
            categories = c
            transactions = t
        } catch {
            presentError(error.localizedDescription)
        }
    }

    // MARK: - Goal CRUD

    func saveGoal(existing: Goal?, title: String, targetAmount: Double, savedAmount: Double, deadline: Date?) async {
        guard let userId else { return }
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        let deadlineMs: Int64? = deadline.map { Int64($0.timeIntervalSince1970 * 1000) }
        let goal = Goal(
            id: existing?.id ?? UUID().uuidString,
            userId: userId,
            title: title,
            targetAmount: targetAmount,
            savedAmount: savedAmount,
            deadline: deadlineMs,
            isCompleted: savedAmount >= targetAmount,
            createdAt: existing?.createdAt ?? now
        )
        do {
            if existing == nil {
                try await goalService.create(goal)
                goals.insert(goal, at: 0)
            } else {
                try await goalService.update(goal)
                if let i = goals.firstIndex(where: { $0.id == goal.id }) {
                    goals[i] = goal
                }
            }
        } catch {
            presentError(error.localizedDescription)
        }
    }

    func deleteGoal(_ goal: Goal) {
        goals.removeAll { $0.id == goal.id }
        Task {
            do {
                try await goalService.delete(goal)
            } catch {
                presentError(error.localizedDescription)
                await load()
            }
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

    // MARK: - Insights

    var todayInsight: GoalInsight? {
        let active = goals.filter { !$0.isCompleted && $0.targetAmount > 0 && $0.savedAmount < $0.targetAmount }
        guard !active.isEmpty else { return nil }

        let now = Date()
        let nowMs = Int64(now.timeIntervalSince1970 * 1000)
        let dayOfYear = Calendar.current.ordinality(of: .day, in: .year, for: now) ?? 1
        let msPerMonth: Double = 1000 * 60 * 60 * 24 * 30.44

        var candidates: [GoalInsight] = []

        for goal in active {
            let remaining = goal.targetAmount - goal.savedAmount
            let monthsSinceCreation = Double(max(0, nowMs - goal.createdAt)) / msPerMonth

            if goal.progress > 0.85 {
                candidates.append(GoalInsight(
                    message: Bundle.L("insight.finish", goal.title, currency(remaining)),
                    icon: "star.fill",
                    accentColor: .orange
                ))
            }

            if let deadline = goal.deadline, deadline > nowMs {
                let totalDuration = Double(deadline - goal.createdAt)
                if totalDuration > 0 {
                    let expectedProgress = min(Double(nowMs - goal.createdAt) / totalDuration, 1.0)
                    if expectedProgress > goal.progress + 0.12 {
                        let daysLeft = Int(Double(deadline - nowMs) / (1000 * 60 * 60 * 24))
                        candidates.append(GoalInsight(
                            message: Bundle.L("insight.behind", goal.title, currency(remaining), daysLeft),
                            icon: "exclamationmark.triangle.fill",
                            accentColor: .orange
                        ))
                    } else if goal.progress > expectedProgress + 0.12 {
                        let pct = Int((goal.progress - expectedProgress) * 100)
                        candidates.append(GoalInsight(
                            message: Bundle.L("insight.ahead", goal.title, pct),
                            icon: "checkmark.seal.fill",
                            accentColor: AppColors.lightGreenFrameColor
                        ))
                    }
                }
            }

            if monthsSinceCreation > 0.5 && goal.savedAmount > 0 && remaining > 0 {
                let monthlyRate = goal.savedAmount / monthsSinceCreation
                if monthlyRate > 100 {
                    let bonus = Double(2000 + (dayOfYear % 5) * 1000)
                    let monthsNow = remaining / monthlyRate
                    let monthsWith = remaining / (monthlyRate + bonus)
                    let monthsSaved = monthsNow - monthsWith
                    if monthsWith <= monthsNow * 0.85 && monthsSaved >= 1 {
                        candidates.append(GoalInsight(
                            message: Bundle.L("insight.bonus", currency(bonus), goal.title, Int(monthsSaved)),
                            icon: "lightbulb.fill",
                            accentColor: AppColors.lightGreenFrameColor
                        ))
                    }
                }
            }
        }

        if candidates.isEmpty {
            let goal = active[dayOfYear % active.count]
            let pct = Int(goal.progress * 100)
            let remaining = goal.targetAmount - goal.savedAmount
            candidates.append(GoalInsight(
                message: pct > 0
                    ? Bundle.L("insight.progress", pct, goal.title, currency(remaining))
                    : Bundle.L("insight.start", goal.title),
                icon: pct > 0 ? "chart.line.uptrend.xyaxis" : "flag.fill",
                accentColor: AppColors.lightGreenFrameColor
            ))
        }

        return candidates[dayOfYear % candidates.count]
    }

    private func currency(_ amount: Double) -> String {
        let f = NumberFormatter()
        f.numberStyle = .currency
        f.currencyCode = "RUB"
        f.locale = Locale(identifier: "ru_RU")
        f.maximumFractionDigits = 0
        return f.string(from: NSNumber(value: amount)) ?? "\(Int(amount)) ₽"
    }

    private func presentError(_ message: String) {
        errorMessage = message
        showError = true
    }
}
