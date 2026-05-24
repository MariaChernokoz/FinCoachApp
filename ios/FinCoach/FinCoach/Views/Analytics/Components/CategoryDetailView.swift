//
//  CategoryDetailView.swift
//  FinCoach
//

import SwiftUI
import Charts

struct CategoryDetailView: View {
    let allBreakdown: [CategorySpending]
    let excludedCategories: Set<String>
    let onToggle: (String) -> Void
    let onReset: () -> Void
    let total: Double
    @Environment(\.dismiss) private var dismiss

    private var hasFilter: Bool { !excludedCategories.isEmpty }

    private var chartBreakdown: [CategorySpending] {
        let visible = allBreakdown.filter { !excludedCategories.contains($0.category) }
        let visibleTotal = visible.reduce(0) { $0 + $1.amount }
        guard visibleTotal > 0 else { return [] }
        return visible.map { item in
            CategorySpending(
                category: item.category,
                icon: item.icon,
                amount: item.amount,
                percentage: item.amount / visibleTotal * 100,
                color: item.color
            )
        }
    }

    private var chartTotal: Double { chartBreakdown.reduce(0) { $0 + $1.amount } }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    // Диаграмма
                    ZStack {
                        if chartBreakdown.isEmpty {
                            Circle()
                                .stroke(AppColors.lightGrayFrameColor, lineWidth: 8)
                                .frame(height: 180)
                        } else {
                            Chart(chartBreakdown) { item in
                                SectorMark(
                                    angle: .value("Сумма", item.amount),
                                    innerRadius: .ratio(0.75),
                                    angularInset: 1.5
                                )
                                .cornerRadius(4)
                                .foregroundStyle(item.color)
                            }
                            .frame(height: 180)
                        }

                        VStack(spacing: 2) {
                            Text(hasFilter ? "Выбрано" : "Итого")
                                .font(.system(size: 12, weight: .regular))
                                .foregroundColor(AppColors.grayTextColor)
                            Text(formatted(chartTotal))
                                .font(.system(size: 22, weight: .bold))
                                .foregroundColor(AppColors.blackTextColor)
                                .minimumScaleFactor(0.6)
                                .lineLimit(1)
                                .frame(maxWidth: 100)
                        }
                    }
                    .padding(.horizontal, 20)
                    .animation(.easeInOut(duration: 0.25), value: excludedCategories)

                    Text("Нажмите на категорию чтобы включить / исключить")
                        .font(.system(size: 12))
                        .foregroundColor(AppColors.grayTextColor)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 20)

                    // Список категорий
                    VStack(spacing: 0) {
                        ForEach(Array(allBreakdown.enumerated()), id: \.element.id) { index, item in
                            let excluded = excludedCategories.contains(item.category)

                            HStack(spacing: 12) {
                                Circle()
                                    .fill(excluded ? AppColors.lightGrayFrameColor.opacity(0.3) : AppColors.lightGreenFrameColor)
                                    .frame(width: 36, height: 36)
                                    .overlay(Circle().stroke(excluded ? Color.clear : item.color, lineWidth: 5))
                                    .overlay(
                                        Image(systemName: item.icon)
                                            .font(.system(size: 15, weight: .semibold))
                                            .foregroundColor(excluded ? AppColors.grayTextColor : AppColors.darkGrayFrameColor)
                                    )
                                Text(LocalizedStringKey(item.category))
                                    .font(.system(size: 15, weight: .regular))
                                    .foregroundColor(AppColors.blackTextColor)
                                    .lineLimit(1)
                                    .strikethrough(excluded, color: AppColors.grayTextColor)
                                Spacer()
                                if !excluded {
                                    Text(String(format: "%.1f%%", item.percentage))
                                        .font(.system(size: 13, weight: .medium))
                                        .foregroundColor(AppColors.grayTextColor)
                                        .frame(width: 44, alignment: .trailing)
                                    Text(formatted(item.amount))
                                        .font(.system(size: 15, weight: .semibold))
                                        .foregroundColor(AppColors.blackTextColor)
                                        .frame(minWidth: 80, alignment: .trailing)
                                }
                            }
                            .padding(.horizontal, 20)
                            .padding(.vertical, 12)
                            .opacity(excluded ? 0.4 : 1)
                            .contentShape(Rectangle())
                            .onTapGesture {
                                withAnimation(.easeInOut(duration: 0.2)) { onToggle(item.category) }
                            }

                            if index < allBreakdown.count - 1 {
                                Divider().padding(.leading, 70)
                            }
                        }
                    }
                    .background(AppColors.whiteFrameColor)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(AppColors.lightGrayFrameColor, lineWidth: 1))
                    .padding(.horizontal, 20)
                    .animation(.easeInOut(duration: 0.25), value: excludedCategories)
                }
                .padding(.top, 16)
                .padding(.bottom, 32)
            }
            .background(AppColors.backgroundColor)
            .navigationTitle("Все категории")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                if hasFilter {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Сбросить") { onReset() }
                            .foregroundColor(AppColors.lightGreenFrameColor)
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Готово") { dismiss() }
                        .fontWeight(.semibold)
                }
            }
        }
    }

    private func formatted(_ amount: Double) -> String {
        let f = NumberFormatter()
        f.numberStyle = .currency
        f.currencyCode = "RUB"
        f.locale = Locale(identifier: "ru_RU")
        f.maximumFractionDigits = 0
        return f.string(from: NSNumber(value: amount)) ?? "\(amount)"
    }
}
