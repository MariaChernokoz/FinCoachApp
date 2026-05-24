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
    @State private var searchText = ""
    @FocusState private var isSearchFocused: Bool

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
                    VStack(alignment: .leading, spacing: 6) {
                        aiCoachSearchBar
                        hintChipsSlider
                    }

                    if viewModel.isLoading {
                        HStack {
                            Spacer()
                            ProgressView()
                                .padding(.vertical, 40)
                            Spacer()
                        }
                    } else {
                        DonutChartView(
                            allBreakdown: viewModel.categoryBreakdown,
                            chartBreakdown: viewModel.filteredCategoryBreakdown,
                            excludedCategories: viewModel.excludedChartCategories,
                            onToggle: { viewModel.toggleChartCategory($0) },
                            onReset: { viewModel.excludedChartCategories = [] },
                            onShowAll: { showAllCategories = true },
                            period: $viewModel.period,
                            averageDailyExpense: viewModel.averageDailyExpense
                        )

                        MonthlyBarChartView(points: viewModel.monthlyPoints)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 18)
                .padding(.bottom, 32)
            }
            .refreshable { await viewModel.load() }
            .background(AppColors.backgroundColor)
            .navigationTitle("Анализ")
            .navigationBarTitleDisplayMode(.inline)
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
        HStack(spacing: 12) {
            Image(systemName: "mic")
                .font(.system(size: 17))
                .foregroundColor(AppColors.grayTextColor)

            TextField("Спросите что-нибудь у AI ассистента", text: $searchText)
                .font(.system(size: 15))
                .foregroundColor(AppColors.blackTextColor)
                .focused($isSearchFocused)
                .onSubmit { sendToAICoach() }

            Button {
                sendToAICoach()
            } label: {
                Image(systemName: "arrow.up.circle.fill")
                    .font(.system(size: 20))
                    .foregroundColor(
                        searchText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                        ? AppColors.grayTextColor : AppColors.lightGreenFrameColor
                    )
            }
            .disabled(searchText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
        }
        .padding(.horizontal, 16)
        .frame(height: 40)
        .background(AppColors.whiteFrameColor)
        .cornerRadius(16)
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(
                    isSearchFocused ? AppColors.lightGreenFrameColor : AppColors.lightGrayFrameColor,
                    lineWidth: 1
                )
        )
    }

    private func sendToAICoach() {
        let text = searchText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else { return }
        searchText = ""
        isSearchFocused = false
        navigationState.navigateToAICoach(with: text)
    }

    private var hintChipsSlider: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(hints, id: \.self) { hint in
                    Button {
                        navigationState.navigateToAICoach(with: hint)
                    } label: {
                        Text(LocalizedStringKey(hint))
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
