//
//  DonutChartView.swift
//  FinCoach
//

import SwiftUI
import Charts

struct DonutChartView: View {
    let allBreakdown: [CategorySpending]
    let chartBreakdown: [CategorySpending]
    let excludedCategories: Set<String>
    let onToggle: (String) -> Void
    let onReset: () -> Void
    let onShowAll: () -> Void
    @Binding var period: AnalyticsPeriod
    let averageDailyExpense: Double

    private var visibleLegendItems: [CategorySpending] { Array(allBreakdown.prefix(4)) }
    private var hasMore: Bool { allBreakdown.count > 5 }
    private var hasFilter: Bool { !excludedCategories.isEmpty }
    private var chartTotal: Double { chartBreakdown.reduce(0) { $0 + $1.amount } }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                Text("Расходы по категориям")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(AppColors.blackTextColor)
                Spacer()
                if hasFilter {
                    Button("Сбросить", action: onReset)
                        .font(.system(size: 13, weight: .medium))
                        .foregroundColor(AppColors.lightGreenFrameColor)
                }
            }

            VStack(alignment: .leading, spacing: 16) {
                PeriodSelectorView(selected: $period)
                HStack(alignment: .center) {
                    Text("Среднее в день:")
                        .font(.system(size: 13))
                        .foregroundColor(AppColors.grayTextColor)
                    Text(formatted(averageDailyExpense))
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(AppColors.blackTextColor)
                    Spacer()
                }

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
                .overlay(alignment: .bottomTrailing) {
                    Image("pig_analysis")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 72, height: 72)
                        .offset(y: 18)
                        .allowsHitTesting(false)
                }

                VStack(spacing: 0) {
                    ForEach(Array(visibleLegendItems.enumerated()), id: \.element.id) { index, item in
                        let excluded = excludedCategories.contains(item.category)

                        legendRow(item, excluded: excluded)
                            .opacity(excluded ? 0.38 : 1)
                            .onTapGesture { withAnimation(.easeInOut(duration: 0.2)) { onToggle(item.category) } }

                        if index < visibleLegendItems.count - 1 {
                            Divider()
                        }
                    }

                    if hasMore {
                        Divider()
                        Button(action: onShowAll) {
                            HStack(spacing: 4) {
                                Text("Все категории")
                                Image(systemName: "chevron.right")
                                    .font(.system(size: 11, weight: .semibold))
                            }
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(AppColors.lightGreenFrameColor)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                        }
                    }
                }

                if hasFilter {
                    Text("Нажмите на категорию чтобы включить / исключить")
                        .font(.system(size: 11))
                        .foregroundColor(AppColors.grayTextColor)
                } else {
                    Text("Нажмите на категорию чтобы исключить её")
                        .font(.system(size: 11))
                        .foregroundColor(AppColors.grayTextColor)
                }
            }
            .padding(20)
            .background(AppColors.whiteFrameColor)
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(AppColors.lightGrayFrameColor, lineWidth: 1))
            .cornerRadius(16)
        }
        .animation(.easeInOut(duration: 0.25), value: excludedCategories)
    }

    private func legendRow(_ item: CategorySpending, excluded: Bool) -> some View {
        HStack(spacing: 12) {
            Circle()
                .fill(excluded ? AppColors.lightGrayFrameColor.opacity(0.5) : AppColors.lightGreenFrameColor)
                .frame(width: 34, height: 34)
                .overlay(Circle().stroke(excluded ? Color.clear : item.color, lineWidth: 4))
                .overlay(
                    Image(systemName: item.icon)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(excluded ? AppColors.grayTextColor : AppColors.darkGrayFrameColor)
                )
            Text(item.category)
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
        .padding(.vertical, 12)
        .contentShape(Rectangle())
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
