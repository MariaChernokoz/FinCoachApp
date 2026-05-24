//
//  TransactionsView.swift
//  FinCoach
//
//  Created by Chernokoz on 09.02.2026.
//

import SwiftUI
import FirebaseAuth

struct TransactionsView: View {
    @EnvironmentObject private var authViewModel: AuthViewModel
    @StateObject private var viewModel = TransactionsViewModel()
    @State private var editorRoute: TransactionEditorRoute?
    @State private var showFilterSheet = false
    @State private var isBalanceHidden = false
    @FocusState private var isSearchFocused: Bool

    var body: some View {
        NavigationStack {
            ZStack(alignment: .bottomTrailing) {
                ShakeDetector {
                    withAnimation {
                        isBalanceHidden.toggle()
                    }
                }
                .allowsHitTesting(false)

                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        searchAndFilter
                        balanceCard
                        transactionsList
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 18)
                    .padding(.bottom, 96)
                }
                .refreshable { await viewModel.loadTransactions() }
                .background(AppColors.backgroundColor)

                Button {
                    editorRoute = TransactionEditorRoute(transaction: nil)
                } label: {
                    Image(systemName: "plus")
                        .font(.system(size: 22, weight: .semibold))
                        .foregroundColor(.white)
                        .frame(width: 58, height: 58)
                        .background(AppColors.lightGreenFrameColor)
                        .clipShape(Circle())
//                        .shadow(color: AppColors.lightGreenFrameColor.opacity(0.35), radius: 14, x: 0, y: 8)
                }
                .padding(.trailing, 24)
                .padding(.bottom, 24)
            }
            .navigationTitle("Транзакции")
            .navigationBarTitleDisplayMode(.inline)
            .onAppear {
                if let userId = authViewModel.currentUser?.uid {
                    viewModel.start(userId: userId)
                }
            }
            .onDisappear {
                viewModel.stop()
            }
            .sheet(item: $editorRoute) { route in
                TransactionEditorView(
                    transaction: route.transaction,
                    categories: viewModel.categories,
                    isSaving: $viewModel.isSaving
                ) { draft in
                    await viewModel.saveTransaction(
                        existing: route.transaction,
                        draft: draft
                    )
                }
            }
            .sheet(isPresented: $showFilterSheet) {
                FilterSheetView(viewModel: viewModel)
            }
            .alert("Ошибка", isPresented: $viewModel.showError) {
                Button("OK", role: .cancel) { }
            } message: {
                Text(viewModel.errorMessage ?? "Произошла ошибка")
            }
        }
    }

    private var searchAndFilter: some View {
        HStack(spacing: 8) {
            HStack(spacing: 8) {
                Image(systemName: "magnifyingglass")
                    .font(.system(size: 16))
                    .foregroundColor(AppColors.grayTextColor)
                TextField("Поиск транзакций", text: $viewModel.searchText)
                    .font(.system(size: 15))
                    .foregroundColor(AppColors.blackTextColor)
                    .textInputAutocapitalization(.never)
                    .disableAutocorrection(true)
                    .focused($isSearchFocused)
            }
            .padding(.horizontal, 16)
            .frame(height: 40)
            .background(AppColors.whiteFrameColor)
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(
                isSearchFocused ? AppColors.lightGreenFrameColor : AppColors.lightGrayFrameColor,
                lineWidth: 1
            ))
            .cornerRadius(16)

            Menu {
                ForEach(SortOption.allCases) { option in
                    Button {
                        viewModel.sortOption = option
                    } label: {
                        HStack {
                            Text(option.rawValue)
                            if viewModel.sortOption == option {
                                Image(systemName: "checkmark")
                            }
                        }
                    }
                }
            } label: {
                Image(systemName: "arrow.up.arrow.down")
                    .font(.system(size: 15, weight: .medium))
                    .foregroundColor(viewModel.sortOption == .dateDescending
                        ? AppColors.grayTextColor : AppColors.darkGreenFrameColor)
                    .frame(width: 40, height: 40)
                    .background(AppColors.whiteFrameColor)
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(
                        viewModel.sortOption == .dateDescending
                            ? AppColors.lightGrayFrameColor : AppColors.lightGreenFrameColor,
                        lineWidth: 1
                    ))
                    .cornerRadius(12)
            }

            Button { showFilterSheet = true } label: {
                ZStack(alignment: .topTrailing) {
                    Image(systemName: "slider.horizontal.3")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(viewModel.hasActiveFilters
                            ? AppColors.darkGreenFrameColor : AppColors.grayTextColor)
                        .frame(width: 40, height: 40)
                        .background(AppColors.whiteFrameColor)
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(
                            viewModel.hasActiveFilters
                                ? AppColors.lightGreenFrameColor : AppColors.lightGrayFrameColor,
                            lineWidth: 1
                        ))
                        .cornerRadius(12)
                    if viewModel.hasActiveFilters {
                        Circle()
                            .fill(AppColors.lightGreenFrameColor)
                            .frame(width: 9, height: 9)
                            .offset(x: 2, y: -2)
                    }
                }
            }
        }
    }

    private var balanceCard: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Text("Баланс")
                    .font(.system(size: 15, weight: .medium))
                    .foregroundColor(AppColors.grayTextColor)
                    //.padding(.bottom, 6)

                Spacer()

                Text(formatAmount(viewModel.filteredBalance, showSign: false))
                    .font(.system(size: 25, weight: .bold))
                    .foregroundColor(AppColors.blackTextColor)
                    .minimumScaleFactor(0.7)
                    .lineLimit(1)
                    .spoiler(isOn: $isBalanceHidden)
            }
            .padding(.horizontal, 20)
            .padding(.top, 16)

            HStack(spacing: 30) {
                OverflowMetricCard(
                    title: "Расходы",
                    amount: viewModel.filteredExpense,
                    imageName: "money"
                )
                OverflowMetricCard(
                    title: "Доходы",
                    amount: viewModel.filteredIncome,
                    imageName: "pig"
                )
            }
            .spoiler(isOn: $isBalanceHidden)
            .padding(.horizontal, 32)
            //.padding(.top, 8)
            .padding(.bottom, 12)
        }
        .background(AppColors.lightGreenFrameColor)
        .cornerRadius(16)
        .clipped(antialiased: false)
    }

    @ViewBuilder
    private var transactionsList: some View {
        if viewModel.isLoading && viewModel.transactions.isEmpty {
            VStack(spacing: 12) {
                ProgressView()
                Text("Загружаем транзакции")
                    .foregroundColor(AppColors.grayTextColor)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 42)
        } else if viewModel.filteredTransactions.isEmpty {
            EmptyTransactionsView {
                editorRoute = TransactionEditorRoute(transaction: nil)
            }
        } else {
            VStack(alignment: .leading, spacing: 18) {
                ForEach(viewModel.groupedTransactions, id: \.title) { section in
                    VStack(alignment: .leading, spacing: 10) {
                        if !section.title.isEmpty {
                            Text(section.title)
                                .font(.system(size: 17, weight: .medium))
                                .foregroundColor(AppColors.darkGrayTextColor)
                        }

                        VStack(spacing: 0) {
                            ForEach(section.transactions) { transaction in
                                TransactionRowView(
                                    transaction: transaction,
                                    categoryTitle: viewModel.categoryTitle(for: transaction),
                                    iconName: viewModel.categoryIcon(for: transaction)
                                )
                                .contentShape(Rectangle())
                                .onTapGesture {
                                    editorRoute = TransactionEditorRoute(transaction: transaction)
                                }
                                .swipeToDelete {
                                    viewModel.delete(transaction)
                                }

                                if transaction.id != section.transactions.last?.id {
                                    Divider()
                                        .padding(.leading, 70)
                                }
                            }
                        }
                        .background(AppColors.whiteFrameColor)
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                        .overlay(
                            RoundedRectangle(cornerRadius: 16)
                                .stroke(AppColors.lightGrayFrameColor, lineWidth: 1)
                        )
                    }
                }
            }
        }
    }

    private func formatAmount(_ amount: Double, showSign: Bool) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.currencyCode = "RUB"
        formatter.locale = Locale(identifier: "ru_RU")
        formatter.maximumFractionDigits = 2

        if showSign {
            let formatted = formatter.string(from: NSNumber(value: abs(amount))) ?? "\(abs(amount))"
            return amount >= 0 ? "+\(formatted)" : "-\(formatted)"
        }
        return formatter.string(from: NSNumber(value: amount)) ?? "\(amount)"
    }
}

#Preview {
    TransactionsView()
        .environmentObject(AuthViewModel())
}
