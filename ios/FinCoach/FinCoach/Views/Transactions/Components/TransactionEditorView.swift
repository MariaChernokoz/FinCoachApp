//
//  TransactionEditorView.swift
//  FinCoach
//
//  Created by Chernokoz on 08.05.2026.
//

import SwiftUI

struct TransactionEditorRoute: Identifiable {
    let id = UUID()
    let transaction: Transaction?
}

struct TransactionEditorView: View {
    let transaction: Transaction?
    let categories: [Category]
    @Binding var isSaving: Bool
    let onSave: @MainActor (TransactionDraft) async -> Bool

    @Environment(\.dismiss) private var dismiss
    @State private var title: String
    @State private var amountText: String
    @State private var type: TransactionType
    @State private var category: String
    @State private var date: Date

    init(
        transaction: Transaction?,
        categories: [Category],
        isSaving: Binding<Bool>,
        onSave: @escaping @MainActor (TransactionDraft) async -> Bool
    ) {
        self.transaction = transaction
        self.categories = categories
        self._isSaving = isSaving
        self.onSave = onSave
        _title = State(initialValue: transaction?.title ?? "")
        _amountText = State(initialValue: transaction.map { String($0.amount) } ?? "")
        _type = State(initialValue: transaction?.isIncome == true ? .income : .expense)
        _category = State(initialValue: transaction?.category ?? "")
        _date = State(initialValue: transaction?.date ?? Date())
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Picker("Тип", selection: $type) {
                        ForEach(TransactionType.allCases) { type in
                            Text(type.title).tag(type)
                        }
                    }
                    .pickerStyle(.segmented)
                    .onChange(of: type) { _, _ in category = "" }
                }

                Section {
                    TextField("Название", text: $title)

                    TextField("Сумма", text: $amountText)
                        .keyboardType(.decimalPad)

                    DatePicker("Дата", selection: $date, displayedComponents: [.date, .hourAndMinute])
                }

                Section {
                    Picker("Категория", selection: $category) {
                        Text("Без категории").tag("")
                        ForEach(categoriesForSelectedType) { category in
                            Label(category.title, systemImage: category.icon)
                                .tag(category.title)
                        }
                    }
                }
            }
            .navigationTitle(transaction == nil ? "Новая операция" : "Редактирование")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Отмена") { dismiss() }
                }

                ToolbarItem(placement: .confirmationAction) {
                    Button {
                        let titleValue   = title.trimmingCharacters(in: .whitespacesAndNewlines)
                        let normalized   = amountText.fcNormalizedAmountText
                        let amountParsed = Double(normalized)

                        guard let amount = amountParsed, amount > 0 else { return }

                        let draft = TransactionDraft(
                            title: titleValue,
                            amount: amount,
                            category: category.trimmingCharacters(in: .whitespacesAndNewlines),
                            isIncome: type == .income,
                            date: date
                        )

                        Task { @MainActor in
                            if await onSave(draft) { dismiss() }
                        }
                    } label: {
                        if isSaving {
                            ProgressView()
                        } else {
                            Text("Сохранить")
                        }
                    }
                    .disabled(isSaving || title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || parsedAmount == nil)
                }
            }
        }
    }

    private var categoriesForSelectedType: [Category] {
        categories.filter { $0.type == type }
    }

    private var parsedAmount: Double? {
        let normalized = amountText.fcNormalizedAmountText
        return Double(normalized).flatMap { $0.isFinite && $0 > 0 ? $0 : nil }
    }
}

private extension String {
    var fcNormalizedAmountText: String {
        let withoutSpaces = filter { !$0.isWhitespace && $0 != "\u{00A0}" && $0 != "\u{202F}" }
        let withDotSeparator = withoutSpaces.replacingOccurrences(of: ",", with: ".")
        let allowedCharacters = Set("0123456789.")
        let filtered = withDotSeparator.filter { allowedCharacters.contains($0) }

        guard let lastDotIndex = filtered.lastIndex(of: ".") else {
            return filtered
        }

        let integerPart = filtered[..<lastDotIndex].filter { $0 != "." }
        let fractionPart = filtered[filtered.index(after: lastDotIndex)...].filter { $0 != "." }

        return fractionPart.isEmpty ? String(integerPart) : "\(integerPart).\(fractionPart)"
    }
}
