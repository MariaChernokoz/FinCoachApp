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

    private let transactionsService = TransactionsService()
    private let categoriesService = CategoriesService()
    private var userId: String?
    
    var filteredTransactions: [Transaction] {
        transactions.filter { transaction in
            let matchesSearch = searchText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ||
            transaction.title.localizedCaseInsensitiveContains(searchText) ||
            transaction.category.localizedCaseInsensitiveContains(searchText)
            
            let matchesType: Bool
            if let selectedType {
                matchesType = transaction.isIncome == (selectedType == .income)
            } else {
                matchesType = true
            }
            
            return matchesSearch && matchesType
        }
    }
    
    var groupedTransactions: [(title: String, transactions: [Transaction])] {
        let calendar = Calendar.current
        let grouped = Dictionary(grouping: filteredTransactions) { transaction in
            calendar.startOfDay(for: transaction.date)
        }
        
        return grouped
            .sorted { $0.key > $1.key }
            .map { date, transactions in
                (Self.sectionTitle(for: date), transactions.sorted { $0.timestamp > $1.timestamp })
            }
    }
    
    var balance: Double {
        transactions.reduce(0) { result, transaction in
            result + (transaction.isIncome ? transaction.amount : -transaction.amount)
        }
    }
    
    var monthIncome: Double {
        currentMonthTransactions
            .filter(\.isIncome)
            .reduce(0) { $0 + $1.amount }
    }
    
    var monthExpense: Double {
        currentMonthTransactions
            .filter { !$0.isIncome }
            .reduce(0) { $0 + $1.amount }
    }
    
    private var currentMonthTransactions: [Transaction] {
        let calendar = Calendar.current
        return transactions.filter { calendar.isDate($0.date, equalTo: Date(), toGranularity: .month) }
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
        print("[FinCoach][VM] saveTransaction | title='\(draft.title)' amount=\(draft.amount) isIncome=\(draft.isIncome) userId=\(userId ?? "nil")")

        guard let userId else {
            print("[FinCoach][VM] FAIL no userId")
            presentError("Пользователь не найден")
            return false
        }

        guard !draft.title.isEmpty else {
            print("[FinCoach][VM] FAIL title empty")
            presentError("Введите название")
            return false
        }

        guard draft.amount.isFinite, draft.amount > 0 else {
            print("[FinCoach][VM] FAIL amount=\(draft.amount) isFinite=\(draft.amount.isFinite)")
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
        
        isSaving = true
        defer { isSaving = false }

        do {
            if transaction == nil {
                try await transactionsService.create(savedTransaction)
            } else {
                try await transactionsService.update(savedTransaction)
            }
            await loadTransactions()
            return true
        } catch {
            presentError(error.localizedDescription)
            return false
        }
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
