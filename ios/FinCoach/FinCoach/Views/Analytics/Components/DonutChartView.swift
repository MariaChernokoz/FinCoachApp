//
//  DonutChartView.swift
//  FinCoach
//

import SwiftUI
import Charts

struct DonutChartView: View {
    let breakdown: [CategorySpending]
    let total: Double
    let onShowAll: () -> Void

    private var visibleItems: [CategorySpending] { Array(breakdown.prefix(5)) }
    private var hasMore: Bool { breakdown.count > 5 }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Расходы по категориям")
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(AppColors.blackTextColor)

            ZStack {
                Chart(breakdown) { item in
                    SectorMark(
                        angle: .value("Сумма", item.amount),
                        innerRadius: .ratio(0.58),
                        angularInset: 1.5
                    )
                    .cornerRadius(4)
                    .foregroundStyle(item.color)
                }
                .frame(height: 210)

                VStack(spacing: 2) {
                    Text("Итого")
                        .font(.system(size: 12, weight: .regular))
                        .foregroundColor(AppColors.grayTextColor)
                    Text(formatted(total))
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(AppColors.blackTextColor)
                        .minimumScaleFactor(0.6)
                        .lineLimit(1)
                        .frame(maxWidth: 110)
                }
            }

            VStack(spacing: 12) {
                ForEach(visibleItems) { item in
                    legendRow(item)
                }

                if hasMore {
                    Button(action: onShowAll) {
                        HStack(spacing: 4) {
                            Text("Все категории")
                                .font(.system(size: 14, weight: .medium))
                            Image(systemName: "chevron.right")
                                .font(.system(size: 12, weight: .medium))
                        }
                        .foregroundColor(AppColors.lightGreenFrameColor)
                    }
                    .padding(.top, 2)
                }
            }
        }
        .padding(20)
        .background(AppColors.whiteFrameColor)
        .overlay(RoundedRectangle(cornerRadius: 22).stroke(AppColors.lightGrayFrameColor, lineWidth: 1))
        .cornerRadius(22)
    }

    private func legendRow(_ item: CategorySpending) -> some View {
        HStack(spacing: 10) {
            RoundedRectangle(cornerRadius: 4)
                .fill(item.color)
                .frame(width: 12, height: 12)
            Image(systemName: item.icon)
                .font(.system(size: 13))
                .foregroundColor(AppColors.grayTextColor)
                .frame(width: 18)
            Text(item.category)
                .font(.system(size: 14, weight: .regular))
                .foregroundColor(AppColors.blackTextColor)
                .lineLimit(1)
            Spacer()
            Text(String(format: "%.1f%%", item.percentage))
                .font(.system(size: 13, weight: .medium))
                .foregroundColor(AppColors.grayTextColor)
                .frame(width: 44, alignment: .trailing)
            Text(formatted(item.amount))
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(AppColors.blackTextColor)
                .frame(minWidth: 80, alignment: .trailing)
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
