//
//  TransactionsViewModel.swift
//  FinCoach
//
//  Created by Chernokoz on 03.05.2026.
//

import Foundation
import Combine

struct TransactionDraft: Equatable, Sendable {
    let title: String
    let amount: Double
    let category: String
    let isIncome: Bool
    let date: Date
}

enum SortOption: String, CaseIterable, Identifiable {
    case dateDescending  = "Сначала новые"
    case dateAscending   = "Сначала старые"
    case amountDescending = "Сначала дорогие"
    case amountAscending  = "Сначала дешёвые"
    var id: String { rawValue }
}

@MainActor
final class TransactionsViewModel: ObservableObject {
    @Published private(set) var transactions: [Transaction] = []
    @Published private(set) var categories: [Category] = []
    @Published var searchText = ""
    @Published var selectedType: TransactionType?
    @Published var isLoading = false
    @Published var isSaving = false
    @Published var errorMessage: String?
    @Published var showError = false
    @Published var sortOption: SortOption = .dateDescending
    @Published var filterStartDate: Date?
    @Published var filterEndDate: Date?
    @Published var selectedCategories: Set<String> = []

    private let transactionsService = TransactionsService()
    private let categoriesService = CategoriesService()
    private var userId: String?
    
    var filteredTransactions: [Transaction] {
        transactions.filter { transaction in
            let query = searchText.trimmingCharacters(in: .whitespacesAndNewlines)
            let matchesSearch = query.isEmpty ||
                fuzzyMatches(query: query, in: transaction.title) ||
                fuzzyMatches(query: query, in: transaction.category)

            let matchesType: Bool
            if let selectedType {
                matchesType = transaction.isIncome == (selectedType == .income)
            } else {
                matchesType = true
            }

            let calendar = Calendar.current
            let matchesDateRange: Bool
            if let start = filterStartDate, let end = filterEndDate {
                let endDay = calendar.date(bySettingHour: 23, minute: 59, second: 59, of: end) ?? end
                matchesDateRange = transaction.date >= start && transaction.date <= endDay
            } else if let start = filterStartDate {
                matchesDateRange = transaction.date >= start
            } else if let end = filterEndDate {
                let endDay = calendar.date(bySettingHour: 23, minute: 59, second: 59, of: end) ?? end
                matchesDateRange = transaction.date <= endDay
            } else {
                matchesDateRange = true
            }

            let matchesCategory = selectedCategories.isEmpty ||
                selectedCategories.contains(transaction.category)

            return matchesSearch && matchesType && matchesDateRange && matchesCategory
        }
    }

    var hasActiveFilters: Bool {
        selectedType != nil || filterStartDate != nil || filterEndDate != nil || !selectedCategories.isEmpty
    }

    func resetFilters() {
        selectedType = nil
        filterStartDate = nil
        filterEndDate = nil
        selectedCategories = []
    }
    
    var groupedTransactions: [(title: String, transactions: [Transaction])] {
        switch sortOption {
        case .amountDescending:
            return [("", filteredTransactions.sorted { $0.amount > $1.amount })]
        case .amountAscending:
            return [("", filteredTransactions.sorted { $0.amount < $1.amount })]
        case .dateDescending, .dateAscending:
            let calendar = Calendar.current
            let grouped = Dictionary(grouping: filteredTransactions) { transaction in
                calendar.startOfDay(for: transaction.date)
            }
            let sortedDates = grouped.keys.sorted { sortOption == .dateAscending ? $0 < $1 : $0 > $1 }
            return sortedDates.map { date in
                let txns = grouped[date] ?? []
                let sorted = sortOption == .dateAscending
                    ? txns.sorted { $0.timestamp < $1.timestamp }
                    : txns.sorted { $0.timestamp > $1.timestamp }
                return (Self.sectionTitle(for: date), sorted)
            }
        }
    }
    
    var balance: Double {
        transactions.reduce(0) { result, transaction in
            result + (transaction.isIncome ? transaction.amount : -transaction.amount)
        }
    }

    var filteredBalance: Double {
        filteredTransactions.reduce(0) { result, transaction in
            result + (transaction.isIncome ? transaction.amount : -transaction.amount)
        }
    }
    
    var filteredIncome: Double {
        filteredTransactions
            .filter(\.isIncome)
            .reduce(0) { $0 + $1.amount }
    }

    var filteredExpense: Double {
        filteredTransactions
            .filter { !$0.isIncome }
            .reduce(0) { $0 + $1.amount }
    }
    
    func start(userId: String) {
        guard self.userId != userId else { return }
        self.userId = userId
        Task { await loadTransactions() }
        Task { await loadCategories() }
    }

    func stop() {
        userId = nil
    }

    func loadTransactions() async {
        guard let userId else { return }
        isLoading = true
        defer { isLoading = false }
        do {
            transactions = try await transactionsService.fetchTransactions(userId: userId)
        } catch {
            presentError(error.localizedDescription)
        }
    }
    
    func loadCategories() async {
        do {
            categories = try await categoriesService.fetchCategories()
        } catch {
            presentError(error.localizedDescription)
        }
    }
    
    func saveTransaction(
        existing transaction: Transaction?,
        draft: TransactionDraft
    ) async -> Bool {
        guard let userId else {
            presentError("Пользователь не найден")
            return false
        }

        guard !draft.title.isEmpty else {
            presentError("Введите название")
            return false
        }

        guard draft.amount.isFinite, draft.amount > 0 else {
            presentError("Введите сумму больше 0")
            return false
        }
        
        let savedTransaction = Transaction(
            id: transaction?.id ?? UUID().uuidString,
            title: draft.title,
            amount: draft.amount,
            category: draft.category.isEmpty ? "Без категории" : draft.category,
            isIncome: draft.isIncome,
            timestamp: Int64(draft.date.timeIntervalSince1970 * 1000),
            userId: userId
        )
        
        if transaction == nil {
            transactions.insert(savedTransaction, at: 0)
        } else if let i = transactions.firstIndex(where: { $0.id == savedTransaction.id }) {
            transactions[i] = savedTransaction
        }

        Task {
            do {
                if transaction == nil {
                    try await transactionsService.create(savedTransaction)
                } else {
                    try await transactionsService.update(savedTransaction)
                }
                await loadTransactions()
            } catch {
                if transaction == nil {
                    transactions.removeAll { $0.id == savedTransaction.id }
                } else if let original = transaction,
                          let i = transactions.firstIndex(where: { $0.id == original.id }) {
                    transactions[i] = original
                }
                presentError(error.localizedDescription)
            }
        }

        return true
    }
    
    func delete(_ transaction: Transaction) {
        transactions.removeAll { $0.id == transaction.id }
        Task {
            do {
                try await transactionsService.delete(transaction)
            } catch {
                presentError(error.localizedDescription)
                await loadTransactions()
            }
        }
    }
    
    func categoryTitle(for transaction: Transaction) -> String {
        if let category = categories.first(where: { $0.id == transaction.category || $0.title == transaction.category }) {
            return category.title
        }
        return transaction.category.isEmpty ? "Без категории" : transaction.category
    }
    
    func categoryIcon(for transaction: Transaction) -> String {
        categories.first(where: { $0.id == transaction.category || $0.title == transaction.category })?.icon ?? defaultIcon(for: transaction)
    }
    
    func defaultIcon(for transaction: Transaction) -> String {
        if transaction.isIncome {
            return "wallet.pass"
        }
        
        let lowercasedCategory = transaction.category.lowercased()
        if lowercasedCategory.contains("food") || lowercasedCategory.contains("еда") || lowercasedCategory.contains("продукт") {
            return "cart"
        }
        if lowercasedCategory.contains("transport") || lowercasedCategory.contains("такси") || lowercasedCategory.contains("транспорт") {
            return "car"
        }
        if lowercasedCategory.contains("coffee") || lowercasedCategory.contains("кофе") {
            return "cup.and.saucer"
        }
        return "creditcard"
    }
    
    private func presentError(_ message: String) {
        errorMessage = message
        showError = true
    }
    
    private static func sectionTitle(for date: Date) -> String {
        let calendar = Calendar.current
        if calendar.isDateInToday(date) {
            return "Сегодня"
        }
        if calendar.isDateInYesterday(date) {
            return "Вчера"
        }
        
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "ru_RU")
        formatter.dateFormat = "d MMMM"
        return formatter.string(from: date)
    }
}
