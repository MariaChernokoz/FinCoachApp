//
//  AnalyticsView.swift
//  FinCoach
//

import SwiftUI
import FirebaseAuth

struct AnalyticsView: View {
    @EnvironmentObject private var authViewModel: AuthViewModel
    @EnvironmentObject private var navigationState: AppNavigationState
    @StateObject private var viewModel = AnalyticsViewModel()
    @State private var showAllCategories = false

    private let hints = [
        "Дай общую статистику моих трат",
        "Почему я трачу так много на еду?",
        "Как оптимизировать мой бюджет?",
        "Успею ли я накопить на цель?",
        "Какие расходы можно сократить?"
    ]

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    aiCoachSearchBar
                    hintChipsSlider
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
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 18)
                .padding(.bottom, 32)
            }
            .refreshable { await viewModel.load() }
            .background(Color(.systemBackground))
            .navigationBarHidden(true)
            .onAppear {
                if let userId = authViewModel.currentUser?.uid {
                    viewModel.start(userId: userId)
                }
            }
            .onDisappear { viewModel.stop() }
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

    private var aiCoachSearchBar: some View {
        Button {
            navigationState.navigateToAICoach()
        } label: {
            HStack(spacing: 12) {
                Image(systemName: "mic")
                    .font(.system(size: 17))
                    .foregroundColor(AppColors.grayTextColor)

                Text("Спросите что-нибудь")
                    .font(.system(size: 15))
                    .foregroundColor(AppColors.grayTextColor)

                Spacer()

                Image(systemName: "arrow.up.circle")
                    .font(.system(size: 20))
                    .foregroundColor(AppColors.grayTextColor)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 13)
            .background(AppColors.whiteFrameColor)
            .cornerRadius(22)
            .overlay(
                RoundedRectangle(cornerRadius: 22)
                    .stroke(AppColors.lightGrayFrameColor, lineWidth: 1)
            )
        }
    }

    private var hintChipsSlider: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(hints, id: \.self) { hint in
                    Button {
                        navigationState.navigateToAICoach(with: hint)
                    } label: {
                        Text(hint)
                            .font(.system(size: 13))
                            .foregroundColor(AppColors.blackTextColor)
                            .lineLimit(1)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                            .background(AppColors.whiteFrameColor)
                            .cornerRadius(18)
                            .overlay(
                                RoundedRectangle(cornerRadius: 18)
                                    .stroke(AppColors.lightGrayFrameColor, lineWidth: 1)
                            )
                    }
                }
            }
        }
    }
}

#Preview {
    AnalyticsView()
        .environmentObject(AuthViewModel())
        .environmentObject(AppNavigationState())
}
