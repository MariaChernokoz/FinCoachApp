//
//  TransactionsView.swift
//  FinCoach
//
//  Created by Chernokoz on 09.02.2026.
//

import SwiftUI
import FirebaseAuth

private struct TransactionEditorRoute: Identifiable {
    let id = UUID()
    let transaction: Transaction?
}

struct TransactionsView: View {
    @EnvironmentObject private var authViewModel: AuthViewModel
    @StateObject private var viewModel = TransactionsViewModel()
    @State private var editorRoute: TransactionEditorRoute?
    @State private var showFilterSheet = false

    var body: some View {
        NavigationStack {
            ZStack(alignment: .bottomTrailing) {
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        header
                        searchAndFilter
                        balanceCard
                        transactionsList
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 18)
                    .padding(.bottom, 96)
                }
                .refreshable { await viewModel.loadTransactions() }
                .background(Color(.systemBackground))
                
                Button {
                    editorRoute = TransactionEditorRoute(transaction: nil)
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
    
    private var header: some View {
        VStack (alignment: .leading) {
            HStack {
                AppIcons.logoLightGreen
                    .resizable()
                    .scaledToFit()
                    .frame(width: 28, height: 28)
                VStack(alignment: .leading, spacing: 6) {
                    Text("Transactions")
                        .font(.system(size: 26, weight: .bold))
                        .foregroundColor(AppColors.blackTextColor)
                }
                
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
            Text("Track, manage and grow your money")
                .font(.system(size: 14, weight: .regular))
                .foregroundColor(AppColors.grayTextColor)
        }
    }
    
    private var searchAndFilter: some View {
        HStack(spacing: 8) {
            HStack(spacing: 8) {
                Image(systemName: "magnifyingglass")
                    .font(.system(size: 16))
                    .foregroundColor(AppColors.grayTextColor)
                TextField("Поиск транзакций", text: $viewModel.searchText)
                    .textInputAutocapitalization(.never)
                    .disableAutocorrection(true)
            }
            .padding(.horizontal, 16)
            .frame(height: 40)
            .background(AppColors.whiteFrameColor)
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(AppColors.lightGrayFrameColor, lineWidth: 1))
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
                Text("Total Balance")
                    .font(.system(size: 15, weight: .medium))
                    .foregroundColor(AppColors.grayTextColor)
                    .padding(.bottom, 12)
                
                Spacer()

                Text(formatAmount(viewModel.filteredBalance, showSign: false))
                    .font(.system(size: 34, weight: .bold))
                    .foregroundColor(AppColors.blackTextColor)
                    .minimumScaleFactor(0.7)
                    .lineLimit(1)
            }
            .padding(.horizontal, 20)
            .padding(.top, 18)

            HStack(spacing: 16) {
                OverflowMetricCard(
                    title: "Расходы",
                    amount: viewModel.monthExpense,
                    icon: "arrow.up.right",
                    color: AppColors.purpleFrameColor
                )
                OverflowMetricCard(
                    title: "Доходы",
                    amount: viewModel.monthIncome,
                    icon: "arrow.down.left",
                    color: AppColors.lightGreenFrameColor
                )
            }
            .padding(.horizontal, 16)
            .padding(.top, 22)
            .padding(.bottom, 10)
        }
        .background(
            LinearGradient(
                colors: [
                    AppColors.lightGreenFrameColor.opacity(0.12),
                    AppColors.whiteFrameColor
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .overlay(
            RoundedRectangle(cornerRadius: 22)
                .stroke(AppColors.lightGreenFrameColor.opacity(0.45), lineWidth: 1)
        )
        .cornerRadius(22)
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
                                        .padding(.leading, 66)
                                }
                            }
                        }
                        .background(AppColors.whiteFrameColor)
                        .clipShape(RoundedRectangle(cornerRadius: 22))
                        .overlay(
                            RoundedRectangle(cornerRadius: 22)
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

private struct OverflowMetricCard: View {
    let title: String
    let amount: Double
    let icon: String
    let color: Color

    var body: some View {
        ZStack(alignment: .top) {
            VStack(spacing: 4) {
                Spacer().frame(height: 36)
                Text(currency(amount))
                    .font(.system(size: 17, weight: .bold))
                    .foregroundColor(AppColors.blackTextColor)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
                Text(title)
                    .font(.system(size: 13, weight: .regular))
                    .foregroundColor(AppColors.grayTextColor)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(AppColors.whiteFrameColor)
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(AppColors.lightGrayFrameColor, lineWidth: 1)
            )
            .cornerRadius(16)
            .padding(.top, 30)

            Circle()
                .fill(color.opacity(0.15))
                .frame(width: 60, height: 60)
                .overlay(
                    Circle().stroke(color.opacity(0.25), lineWidth: 1)
                )
                .overlay(
                    Image(systemName: icon)
                        .font(.system(size: 22, weight: .semibold))
                        .foregroundColor(color)
                )
        }
    }

    private func currency(_ amount: Double) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.currencyCode = "RUB"
        formatter.locale = Locale(identifier: "ru_RU")
        formatter.maximumFractionDigits = 0
        return formatter.string(from: NSNumber(value: amount)) ?? "\(amount)"
    }
}

private struct TransactionRowView: View {
    let transaction: Transaction
    let categoryTitle: String
    let iconName: String
    
    var body: some View {
        HStack(spacing: 16) {
            Circle()
                .fill(iconColor.opacity(0.2))
                .frame(width: 48, height: 48)
                .overlay(
                    Image(systemName: iconName)
                        .font(.system(size: 22, weight: .semibold))
                        .foregroundColor(iconColor)
                )
            
            VStack(alignment: .leading, spacing: 7) {
                Text(transaction.title)
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(AppColors.blackTextColor)
                    .lineLimit(1)
                
                HStack(spacing: 7) {
                    Circle()
                        .fill(AppColors.lightGreenFrameColor)
                        .frame(width: 8, height: 8)
                    
                    Text(categoryTitle)
                        .font(.system(size: 14, weight: .regular))
                        .foregroundColor(AppColors.grayTextColor)
                        .lineLimit(1)
                }
            }
            
            Spacer(minLength: 12)
            
            VStack(alignment: .trailing, spacing: 7) {
                Text(amountText)
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(transaction.isIncome ? AppColors.darkGreenFrameColor : AppColors.blackTextColor)
                    .lineLimit(1)
                    .minimumScaleFactor(0.72)
                
                Text(timeText)
                    .font(.system(size: 14, weight: .regular))
                    .foregroundColor(AppColors.grayTextColor)
            }
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 16)
    }
    
    private var iconColor: Color {
        transaction.isIncome ? AppColors.lightGreenFrameColor : AppColors.purpleFrameColor
    }
    
    private var amountText: String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.currencyCode = "RUB"
        formatter.locale = Locale(identifier: "ru_RU")
        formatter.maximumFractionDigits = 2
        let amount = formatter.string(from: NSNumber(value: transaction.amount)) ?? "\(transaction.amount)"
        return transaction.isIncome ? "+\(amount)" : "-\(amount)"
    }
    
    private var timeText: String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "ru_RU")
        formatter.timeStyle = .short
        return formatter.string(from: transaction.date)
    }
}

private struct EmptyTransactionsView: View {
    let addAction: () -> Void
    
    var body: some View {
        VStack(spacing: 16) {
            Circle()
                .fill(AppColors.lightGreenFrameColor.opacity(0.14))
                .frame(width: 72, height: 72)
                .overlay(
                    Image(systemName: "creditcard")
                        .font(.system(size: 28, weight: .semibold))
                        .foregroundColor(AppColors.lightGreenFrameColor)
                )
            
            VStack(spacing: 6) {
                Text("Транзакций пока нет")
                    .font(.system(size: 20, weight: .semibold))
                Text("Добавьте первый доход или расход")
                    .font(.subheadline)
                    .foregroundColor(AppColors.grayTextColor)
            }
            
            Button {
                addAction()
            } label: {
                Label("Добавить", systemImage: "plus")
                    .font(.headline)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 50)
                    .background(AppColors.lightGreenFrameColor)
                    .cornerRadius(16)
            }
        }
        .padding(24)
        .frame(maxWidth: .infinity)
        .background(AppColors.whiteFrameColor)
        .overlay(
            RoundedRectangle(cornerRadius: 22)
                .stroke(AppColors.lightGrayFrameColor, lineWidth: 1)
        )
        .cornerRadius(22)
    }
}

private struct TransactionEditorView: View {
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
                    Button("Отмена") {
                        dismiss()
                    }
                }
                
                ToolbarItem(placement: .confirmationAction) {
                    Button {
                        let titleValue    = title.trimmingCharacters(in: .whitespacesAndNewlines)
                        let normalized    = amountText.fcNormalizedAmountText
                        let amountParsed  = Double(normalized)

                        print("[FinCoach][Editor] Save pressed | title='\(titleValue)' raw='\(amountText)' normalized='\(normalized)' parsed=\(String(describing: amountParsed))")

                        guard let amount = amountParsed, amount > 0 else {
                            print("[FinCoach][Editor] amount invalid — skipping")
                            return
                        }

                        let draft = TransactionDraft(
                            title: titleValue,
                            amount: amount,
                            category: category.trimmingCharacters(in: .whitespacesAndNewlines),
                            isIncome: type == .income,
                            date: date
                        )

                        print("[FinCoach][Editor] draft ready | amount=\(draft.amount) isIncome=\(draft.isIncome)")

                        Task { @MainActor in
                            let saved = await onSave(draft)
                            print("[FinCoach][Editor] onSave result=\(saved)")
                            if saved { dismiss() }
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

        // "1200," → "1200." → fractionPart пуст → возвращаем без точки,
        // иначе Double("1200.") = nil и кнопка заблокирована
        return fractionPart.isEmpty ? String(integerPart) : "\(integerPart).\(fractionPart)"
    }
}

private struct SwipeToDeleteModifier: ViewModifier {
    let onDelete: () -> Void
    @State private var offset: CGFloat = 0
    private let threshold: CGFloat = 75

    func body(content: Content) -> some View {
        ZStack(alignment: .trailing) {
            Color.red
                .frame(width: max(0, -offset))
                .overlay(
                    Image(systemName: "trash")
                        .foregroundColor(.white)
                        .font(.system(size: 18, weight: .semibold))
                        .opacity(min(1, -offset / 30))
                        .padding(.trailing, 22),
                    alignment: .trailing
                )

            content
                .offset(x: offset)
                .gesture(
                    DragGesture(minimumDistance: 15, coordinateSpace: .local)
                        .onChanged { value in
                            guard value.translation.width < 0 else { return }
                            offset = max(value.translation.width, -threshold)
                        }
                        .onEnded { value in
                            if value.translation.width < -(threshold * 0.55) {
                                withAnimation(.easeIn(duration: 0.22)) { offset = -400 }
                                DispatchQueue.main.asyncAfter(deadline: .now() + 0.22) { onDelete() }
                            } else {
                                withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) { offset = 0 }
                            }
                        }
                )
        }
        .clipped()
    }
}

private extension View {
    func swipeToDelete(onDelete: @escaping () -> Void) -> some View {
        modifier(SwipeToDeleteModifier(onDelete: onDelete))
    }
}

private struct FilterSheetView: View {
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

#Preview {
    TransactionsView()
        .environmentObject(AuthViewModel())
}
