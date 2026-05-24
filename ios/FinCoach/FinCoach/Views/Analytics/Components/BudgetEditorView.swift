//
//  BudgetEditorView.swift
//  FinCoach
//

import SwiftUI

struct BudgetEditorView: View {
    let budget: Budget?
    let categories: [Category]
    let onSave: @MainActor (String, Double) async -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var selectedCategory: Category?
    @State private var amountText: String = ""
    @State private var isSaving = false

    init(
        budget: Budget?,
        categories: [Category],
        onSave: @escaping @MainActor (String, Double) async -> Void
    ) {
        self.budget = budget
        self.categories = categories
        self.onSave = onSave
        if let budget {
            _amountText = State(initialValue: String(format: "%.0f", budget.limitAmount))
        }
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Категория") {
                    Picker("Категория", selection: $selectedCategory) {
                        Text("Выберите категорию").tag(Optional<Category>.none)
                        ForEach(categories) { cat in
                            Label(LocalizedStringKey(cat.title), systemImage: cat.icon)
                                .tag(Optional<Category>.some(cat))
                        }
                    }
                }

                Section("Лимит в месяц") {
                    TextField("Сумма", text: $amountText)
                        .keyboardType(.decimalPad)
                }
            }
            .navigationTitle(LocalizedStringKey(budget == nil ? "Новый бюджет" : "Редактировать бюджет"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Отмена") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Сохранить") {
                        guard let cat = selectedCategory, let amount = parsedAmount else { return }
                        isSaving = true
                        Task { @MainActor in
                            await onSave(cat.id, amount)
                            dismiss()
                        }
                    }
                    .fontWeight(.semibold)
                    .disabled(selectedCategory == nil || parsedAmount == nil || isSaving)
                }
            }
            .onAppear {
                if let budget {
                    selectedCategory = categories.first {
                        $0.id == budget.categoryId || $0.title == budget.categoryId
                    }
                }
            }
        }
    }

    private var parsedAmount: Double? {
        Double(amountText.replacingOccurrences(of: ",", with: "."))
            .flatMap { $0 > 0 ? $0 : nil }
    }
}
