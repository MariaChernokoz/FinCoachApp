//
//  AnalyticsView.swift
//  FinCoach
//

import SwiftUI
import FirebaseAuth

struct AnalyticsView: View {
    @EnvironmentObject private var authViewModel: AuthViewModel
    @StateObject private var viewModel = AnalyticsViewModel()
    @State private var showBudgetEditor = false
    @State private var editingBudget: Budget?
    @State private var showAllCategories = false

    var body: some View {
        NavigationStack {
            ZStack(alignment: .bottomTrailing) {
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        // header
                        PeriodSelectorView(selected: $viewModel.period)
                        AnalyticsSummaryCards(viewModel: viewModel)

                        if viewModel.isLoading {
                            HStack {
                                Spacer()
                                ProgressView()
                                    .padding(.vertical, 40)
                                Spacer()
                            }
                        } else {
                            if !viewModel.categoryBreakdown.isEmpty {
                                DonutChartView(
                                    allBreakdown: viewModel.categoryBreakdown,
                                    chartBreakdown: viewModel.filteredCategoryBreakdown,
                                    excludedCategories: viewModel.excludedChartCategories,
                                    onToggle: { viewModel.toggleChartCategory($0) },
                                    onReset: { viewModel.excludedChartCategories = [] },
                                    onShowAll: { showAllCategories = true }
                                )
                            }

                            MonthlyBarChartView(points: viewModel.monthlyPoints)

                            BudgetSectionView(
                                viewModel: viewModel,
                                onAdd: { showBudgetEditor = true },
                                onEdit: { editingBudget = $0; showBudgetEditor = true },
                                onDelete: { viewModel.deleteBudget($0) }
                            )
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 18)
                    .padding(.bottom, 96)
                }
                .refreshable { await viewModel.load() }
                .background(Color(.systemBackground))

                Button {
                    showBudgetEditor = true
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
            .navigationBarHidden(true)
            .onAppear {
                if let userId = authViewModel.currentUser?.uid {
                    viewModel.start(userId: userId)
                }
            }
            .onDisappear { viewModel.stop() }
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
            .sheet(isPresented: $showAllCategories) {
                CategoryDetailView(
                    allBreakdown: viewModel.categoryBreakdown,
                    excludedCategories: viewModel.excludedChartCategories,
                    onToggle: { viewModel.toggleChartCategory($0) },
                    onReset: { viewModel.excludedChartCategories = [] },
                    total: viewModel.periodExpense
                )
            }
            .alert("Ошибка", isPresented: $viewModel.showError) {
                Button("OK", role: .cancel) { }
            } message: {
                Text(viewModel.errorMessage ?? "Произошла ошибка")
            }
        }
    }

    private var header: some View {
        VStack(alignment: .leading) {
            HStack {
                AppIcons.logoLightGreen
                    .resizable()
                    .scaledToFit()
                    .frame(width: 28, height: 28)
                Text("Analytics")
                    .font(.system(size: 26, weight: .bold))
                    .foregroundColor(AppColors.blackTextColor)

                Spacer()

                Circle()
                    .fill(AppColors.lightGreenFrameColor.opacity(0.5))
                    .frame(width: 36, height: 36)
                    .overlay(
                        Image(systemName: "person")
                            .font(.system(size: 20, weight: .medium))
                            .foregroundColor(AppColors.whiteFrameColor)
                    )
            }
            Text("Insights into your spending")
                .font(.system(size: 14, weight: .regular))
                .foregroundColor(AppColors.grayTextColor)
        }
    }
}

#Preview {
    AnalyticsView()
        .environmentObject(AuthViewModel())
}
