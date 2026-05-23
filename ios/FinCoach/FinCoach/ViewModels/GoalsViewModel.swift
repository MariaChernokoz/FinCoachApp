//
//  GoalsViewModel.swift
//  FinCoach
//

import Foundation
import SwiftUI
import Combine

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

    private func presentError(_ message: String) {
        errorMessage = message
        showError = true
    }
}
