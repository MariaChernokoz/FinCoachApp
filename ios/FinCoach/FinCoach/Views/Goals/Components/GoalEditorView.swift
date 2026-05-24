//
//  GoalEditorView.swift
//  FinCoach
//

import SwiftUI

struct GoalEditorView: View {
    let goal: Goal?
    let onSave: @MainActor (String, Double, Double, Date?) async -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var title: String = ""
    @State private var targetText: String = ""
    @State private var savedText: String = ""
    @State private var hasDeadline: Bool = false
    @State private var deadline: Date = Calendar.current.date(byAdding: .month, value: 6, to: Date()) ?? Date()
    @State private var isSaving = false

    init(goal: Goal?, onSave: @escaping @MainActor (String, Double, Double, Date?) async -> Void) {
        self.goal = goal
        self.onSave = onSave
        if let goal {
            _title = State(initialValue: goal.title)
            _targetText = State(initialValue: String(format: "%.0f", goal.targetAmount))
            _savedText = State(initialValue: String(format: "%.0f", goal.savedAmount))
            if let dl = goal.deadline {
                let date = Date(timeIntervalSince1970: Double(dl) / 1000)
                _hasDeadline = State(initialValue: true)
                _deadline = State(initialValue: date)
            }
        }
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Название цели") {
                    TextField("Например: Отпуск, Машина...", text: $title)
                }

                Section("Суммы") {
                    HStack {
                        Text("Цель накопления")
                        Spacer()
                        TextField("0", text: $targetText)
                            .keyboardType(.decimalPad)
                            .multilineTextAlignment(.trailing)
                    }
                    HStack {
                        Text("Уже накоплено")
                        Spacer()
                        TextField("0", text: $savedText)
                            .keyboardType(.decimalPad)
                            .multilineTextAlignment(.trailing)
                    }
                }

                Section("Дедлайн") {
                    Toggle("Установить срок", isOn: $hasDeadline.animation())
                    if hasDeadline {
                        DatePicker(
                            "Дата",
                            selection: $deadline,
                            in: Date()...,
                            displayedComponents: .date
                        )
                        .environment(\.locale, Locale(identifier: "ru_RU"))
                    }
                }
            }
            .navigationTitle(LocalizedStringKey(goal == nil ? "Новая цель" : "Редактировать цель"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Отмена") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Сохранить") {
                        guard canSave, let target = parsedAmount(targetText) else { return }
                        let saved = parsedAmount(savedText) ?? 0
                        isSaving = true
                        Task { @MainActor in
                            await onSave(
                                title.trimmingCharacters(in: .whitespaces),
                                target,
                                saved,
                                hasDeadline ? deadline : nil
                            )
                            dismiss()
                        }
                    }
                    .fontWeight(.semibold)
                    .disabled(!canSave || isSaving)
                }
            }
        }
    }

    private var canSave: Bool {
        !title.trimmingCharacters(in: .whitespaces).isEmpty && parsedAmount(targetText) != nil
    }

    private func parsedAmount(_ text: String) -> Double? {
        Double(text.replacingOccurrences(of: ",", with: "."))
            .flatMap { $0 > 0 ? $0 : nil }
    }
}
