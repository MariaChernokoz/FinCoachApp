//
//  GoalsView.swift
//  FinCoach
//

import SwiftUI
import FirebaseAuth

struct GoalsView: View {
    @EnvironmentObject private var authViewModel: AuthViewModel
    @StateObject private var viewModel = GoalsViewModel()
    @State private var showGoalEditor = false
    @State private var editingGoal: Goal?
    @State private var showBudgetEditor = false
    @State private var editingBudget: Budget?

    var body: some View {
        NavigationStack {
            ZStack(alignment: .bottomTrailing) {
                ScrollView {
                    VStack(alignment: .leading, spacing: 20) {
                        goalsSection
                        budgetSection
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 18)
                    .padding(.bottom, 96)
                }
                .refreshable { await viewModel.load() }
                .background(AppColors.backgroundGray)

                Button {
                    editingGoal = nil
                    showGoalEditor = true
                } label: {
                    Image(systemName: "plus")
                        .font(.system(size: 22, weight: .semibold))
                        .foregroundColor(.white)
                        .frame(width: 58, height: 58)
                        .background(AppColors.lightGreenFrameColor)
                        .clipShape(Circle())
                        .shadow(color: AppColors.lightGreenFrameColor.opacity(0.35), radius: 14, x: 0, y: 8)
                }
                .padding(.trailing, 24)
                .padding(.bottom, 24)
            }
            .navigationTitle("Цели и бюджеты")
            .navigationBarTitleDisplayMode(.inline)
            .onAppear {
                if let userId = authViewModel.currentUser?.uid {
                    viewModel.start(userId: userId)
                }
            }
            .onDisappear { viewModel.stop() }
            .sheet(isPresented: $showGoalEditor, onDismiss: { editingGoal = nil }) {
                GoalEditorView(goal: editingGoal) { title, target, saved, deadline in
                    await viewModel.saveGoal(
                        existing: editingGoal,
                        title: title,
                        targetAmount: target,
                        savedAmount: saved,
                        deadline: deadline
                    )
                }
            }
            .sheet(isPresented: $showBudgetEditor, onDismiss: { editingBudget = nil }) {
                BudgetEditorView(
                    budget: editingBudget,
                    categories: viewModel.categories.filter { $0.type == .expense }
                ) { categoryId, amount in
                    await viewModel.saveBudget(
                        existing: editingBudget,
                        categoryId: categoryId,
                        limitAmount: amount
                    )
                }
            }
            .alert("Ошибка", isPresented: $viewModel.showError) {
                Button("OK", role: .cancel) { }
            } message: {
                Text(viewModel.errorMessage ?? "Произошла ошибка")
            }
        }
    }

    // MARK: - Goals section

    private var goalsSection: some View {
        VStack(alignment: .leading, spacing: 14) {
            if viewModel.isLoading {
                HStack {
                    Spacer()
                    ProgressView().padding(.vertical, 40)
                    Spacer()
                }
            } else if viewModel.goals.isEmpty {
                emptyGoalsView
            } else {
                VStack(spacing: 0) {
                    ForEach(viewModel.goals) { goal in
                        GoalCardView(
                            goal: goal,
                            onEdit: {
                                editingGoal = goal
                                showGoalEditor = true
                            },
                            onDelete: { viewModel.deleteGoal(goal) }
                        )
                        if goal.id != viewModel.goals.last?.id {
                            Divider().padding(.leading, 90)
                        }
                    }
                }
                .background(AppColors.whiteFrameColor)
                .clipShape(RoundedRectangle(cornerRadius: 22))
                .overlay(RoundedRectangle(cornerRadius: 22).stroke(AppColors.lightGrayFrameColor, lineWidth: 1))
            }
        }
    }

    private var emptyGoalsView: some View {
        VStack(spacing: 12) {
            Image(systemName: "target")
                .font(.system(size: 34, weight: .light))
                .foregroundColor(AppColors.lightGreenFrameColor.opacity(0.7))
            Text("Нет финансовых целей")
                .font(.system(size: 16, weight: .medium))
                .foregroundColor(AppColors.grayTextColor)
            Text("Создайте цель и следите\nза прогрессом накоплений")
                .font(.system(size: 13))
                .foregroundColor(AppColors.grayTextColor)
                .multilineTextAlignment(.center)
            Button {
                editingGoal = nil
                showGoalEditor = true
            } label: {
                Label("Создать цель", systemImage: "plus")
                    .font(.headline)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 48)
                    .background(AppColors.lightGreenFrameColor)
                    .cornerRadius(14)
            }
        }
        .padding(24)
        .frame(maxWidth: .infinity)
        .background(AppColors.whiteFrameColor)
        .overlay(RoundedRectangle(cornerRadius: 22).stroke(AppColors.lightGrayFrameColor, lineWidth: 1))
        .cornerRadius(22)
    }

    // MARK: - Budget section

    private var budgetSection: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                Text("Бюджеты")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(AppColors.blackTextColor)
                Spacer()
                Button {
                    editingBudget = nil
                    showBudgetEditor = true
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: "plus")
                        Text("Добавить")
                    }
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(AppColors.lightGreenFrameColor)
                }
            }

            if !viewModel.isLoading && viewModel.budgets.isEmpty {
                emptyBudgetsView
            } else {
                VStack(spacing: 0) {
                    ForEach(viewModel.budgets) { budget in
                        let cat = viewModel.category(for: budget)
                        BudgetCardView(
                            budget: budget,
                            categoryTitle: cat?.title ?? budget.categoryId,
                            categoryIcon: cat?.icon ?? "tag",
                            spent: viewModel.currentMonthSpent(for: budget),
                            fraction: viewModel.spentFraction(for: budget),
                            onEdit: {
                                editingBudget = budget
                                showBudgetEditor = true
                            },
                            onDelete: { viewModel.deleteBudget(budget) }
                        )
                        if budget.id != viewModel.budgets.last?.id {
                            Divider().padding(.leading, 82)
                        }
                    }
                }
                .background(AppColors.whiteFrameColor)
                .clipShape(RoundedRectangle(cornerRadius: 22))
                .overlay(RoundedRectangle(cornerRadius: 22).stroke(AppColors.lightGrayFrameColor, lineWidth: 1))
            }
        }
    }

    private var emptyBudgetsView: some View {
        VStack(spacing: 12) {
            Image(systemName: "list.bullet.rectangle")
                .font(.system(size: 34, weight: .light))
                .foregroundColor(AppColors.lightGreenFrameColor.opacity(0.7))
            Text("Бюджеты не заданы")
                .font(.system(size: 16, weight: .medium))
                .foregroundColor(AppColors.grayTextColor)
            Text("Добавьте лимит по категории,\nчтобы контролировать траты")
                .font(.system(size: 13))
                .foregroundColor(AppColors.grayTextColor)
                .multilineTextAlignment(.center)
            Button {
                editingBudget = nil
                showBudgetEditor = true
            } label: {
                Label("Добавить бюджет", systemImage: "plus")
                    .font(.headline)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 48)
                    .background(AppColors.lightGreenFrameColor)
                    .cornerRadius(14)
            }
        }
        .padding(24)
        .frame(maxWidth: .infinity)
        .background(AppColors.whiteFrameColor)
        .overlay(RoundedRectangle(cornerRadius: 22).stroke(AppColors.lightGrayFrameColor, lineWidth: 1))
        .cornerRadius(22)
    }
}

#Preview {
    GoalsView()
        .environmentObject(AuthViewModel())
}
