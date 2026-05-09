//
//  FilterSheetView.swift
//  FinCoach
//
//  Created by Chernokoz on 08.05.2026.
//

import SwiftUI

struct FilterSheetView: View {
    @ObservedObject var viewModel: TransactionsViewModel
    @Environment(\.dismiss) private var dismiss

    private enum DateField { case start, end }

    @State private var localType: TransactionType?
    @State private var localStart: Date?
    @State private var localEnd: Date?
    @State private var localCategories: Set<String> = []
    @State private var activeDateField: DateField?

    private var visibleCategories: [Category] {
        guard let type = localType else { return viewModel.categories }
        return viewModel.categories.filter { $0.type == type }
    }

    private var endOfToday: Date {
        Calendar.current.date(bySettingHour: 23, minute: 59, second: 59, of: Date()) ?? Date()
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Тип операции") {
                    Picker("Тип", selection: $localType) {
                        Text("Все").tag(Optional<TransactionType>.none)
                        Text("Доходы").tag(Optional<TransactionType>.some(.income))
                        Text("Расходы").tag(Optional<TransactionType>.some(.expense))
                    }
                    .pickerStyle(.segmented)
                    .onChange(of: localType) { _, _ in
                        localCategories = localCategories.filter { title in
                            visibleCategories.contains { $0.title == title }
                        }
                        activeDateField = nil
                    }
                }

                Section("Период") {
                    HStack(spacing: 0) {
                        dateCell(label: "Начало", date: localStart, field: .start) {
                            localStart = nil
                            if activeDateField == .start { activeDateField = nil }
                        }
                        Divider().frame(height: 44)
                        dateCell(label: "Конец", date: localEnd, field: .end) {
                            localEnd = nil
                            if activeDateField == .end { activeDateField = nil }
                        }
                        .padding(.leading, 12)
                    }

                    if activeDateField == .start {
                        DatePicker("", selection: Binding(
                            get: { localStart ?? Calendar.current.startOfDay(for: Date()) },
                            set: { newDate in
                                localStart = newDate
                                if let end = localEnd, newDate > end { localEnd = newDate }
                                activeDateField = nil
                            }
                        ), in: ...endOfToday, displayedComponents: .date)
                        .datePickerStyle(.graphical)
                        .labelsHidden()
                    }

                    if activeDateField == .end {
                        DatePicker("", selection: Binding(
                            get: { localEnd ?? Calendar.current.startOfDay(for: Date()) },
                            set: { newDate in
                                localEnd = newDate
                                if let start = localStart, newDate < start { localStart = newDate }
                                activeDateField = nil
                            }
                        ), in: ...endOfToday, displayedComponents: .date)
                        .datePickerStyle(.graphical)
                        .labelsHidden()
                    }
                }

                if !visibleCategories.isEmpty {
                    Section("Категории") {
                        ForEach(visibleCategories) { category in
                            HStack {
                                Image(systemName: category.icon)
                                    .foregroundColor(AppColors.lightGreenFrameColor)
                                    .frame(width: 24)
                                Text(category.title)
                                Spacer()
                                if localCategories.contains(category.title) {
                                    Image(systemName: "checkmark")
                                        .foregroundColor(AppColors.lightGreenFrameColor)
                                        .fontWeight(.semibold)
                                }
                            }
                            .contentShape(Rectangle())
                            .onTapGesture {
                                if localCategories.contains(category.title) {
                                    localCategories.remove(category.title)
                                } else {
                                    localCategories.insert(category.title)
                                }
                            }
                        }
                    }
                }

                Section {
                    Button("Сбросить фильтры", role: .destructive) {
                        viewModel.resetFilters()
                        dismiss()
                    }
                }
            }
            .navigationTitle("Фильтры")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Отмена") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Применить") {
                        viewModel.selectedType = localType
                        viewModel.filterStartDate = localStart
                        viewModel.filterEndDate = localEnd
                        viewModel.selectedCategories = localCategories
                        dismiss()
                    }
                    .fontWeight(.semibold)
                }
            }
            .onAppear {
                localType = viewModel.selectedType
                localStart = viewModel.filterStartDate
                localEnd = viewModel.filterEndDate
                localCategories = viewModel.selectedCategories
            }
        }
    }

    @ViewBuilder
    private func dateCell(label: String, date: Date?, field: DateField, onClear: @escaping () -> Void) -> some View {
        let isActive = activeDateField == field
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(label)
                    .font(.caption2)
                    .foregroundColor(.secondary)
                if let date {
                    HStack(spacing: 4) {
                        Text(formatted(date))
                            .font(.subheadline)
                            .foregroundColor(isActive ? AppColors.lightGreenFrameColor : .primary)
                        Button(action: onClear) {
                            Image(systemName: "xmark.circle.fill")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        .buttonStyle(.plain)
                    }
                } else {
                    Text("—")
                        .font(.subheadline)
                        .foregroundColor(isActive ? AppColors.lightGreenFrameColor : .secondary)
                }
            }
            Spacer()
        }
        .padding(.vertical, 6)
        .frame(maxWidth: .infinity)
        .contentShape(Rectangle())
        .onTapGesture {
            withAnimation(.easeInOut(duration: 0.2)) {
                if isActive {
                    activeDateField = nil
                } else {
                    let today = Calendar.current.startOfDay(for: Date())
                    if field == .start && localStart == nil { localStart = today }
                    if field == .end   && localEnd == nil   { localEnd = today }
                    activeDateField = field
                }
            }
        }
    }

    private func formatted(_ date: Date) -> String {
        let f = DateFormatter()
        f.locale = Locale(identifier: "ru_RU")
        f.dateFormat = "d MMM yyyy"
        return f.string(from: date)
    }
}
