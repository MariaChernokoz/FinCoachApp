//
//  BudgetCardView.swift
//  FinCoach
//

import SwiftUI

struct BudgetCardView: View {
    let budget: Budget
    let categoryTitle: String
    let categoryIcon: String
    let spent: Double
    let fraction: Double
    let onEdit: () -> Void
    let onDelete: () -> Void

    var body: some View {
        HStack(spacing: 16) {
            Circle()
                .fill(AppColors.lightGreenFrameColor.opacity(0.65))
                .frame(width: 44, height: 44)
                .overlay(
                    Image(systemName: categoryIcon)
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(AppColors.darkGrayTextColor)
                )

            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text(categoryTitle)
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundColor(AppColors.blackTextColor)
                        .lineLimit(1)
                    Spacer()
                    Text("\(formatted(spent)) / \(formatted(budget.limitAmount))")
                        .font(.system(size: 13, weight: .medium))
                        .foregroundColor(AppColors.grayTextColor)
                        .lineLimit(1)
                        .minimumScaleFactor(0.8)
                }

                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        RoundedRectangle(cornerRadius: 4)
                            .fill(AppColors.lightGrayFrameColor)
                            .frame(height: 6)
                        RoundedRectangle(cornerRadius: 4)
                            .fill(progressColor)
                            .frame(width: max(0, geo.size.width * fraction), height: 6)
                            .animation(.easeOut(duration: 0.4), value: fraction)
                    }
                }
                .frame(height: 6)

                if fraction >= 1.0 {
                    Text("Лимит превышен")
                        .font(.system(size: 11, weight: .medium))
                        .foregroundColor(.red)
                } else if fraction >= 0.7 {
                    Text(String(format: "Осталось %.0f%%", (1 - fraction) * 100))
                        .font(.system(size: 11, weight: .medium))
                        .foregroundColor(.orange)
                }
            }
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 16)
        .contentShape(Rectangle())
        .onTapGesture { onEdit() }
        .swipeToDelete { onDelete() }
    }

    private var progressColor: Color {
        if fraction >= 1.0 { return .red }
        if fraction >= 0.7 { return .orange }
        return AppColors.lightGreenFrameColor
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
