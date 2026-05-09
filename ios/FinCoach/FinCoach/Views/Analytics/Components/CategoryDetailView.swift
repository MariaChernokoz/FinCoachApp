//
//  CategoryDetailView.swift
//  FinCoach
//

import SwiftUI
import Charts

struct CategoryDetailView: View {
    let breakdown: [CategorySpending]
    let total: Double
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    Chart(breakdown) { item in
                        SectorMark(
                            angle: .value("Сумма", item.amount),
                            innerRadius: .ratio(0.58),
                            angularInset: 1.5
                        )
                        .cornerRadius(4)
                        .foregroundStyle(item.color)
                    }
                    .frame(height: 220)
                    .padding(.horizontal, 20)

                    VStack(spacing: 0) {
                        ForEach(Array(breakdown.enumerated()), id: \.element.id) { index, item in
                            HStack(spacing: 14) {
                                RoundedRectangle(cornerRadius: 4)
                                    .fill(item.color)
                                    .frame(width: 12, height: 12)
                                Circle()
                                    .fill(item.color.opacity(0.15))
                                    .frame(width: 36, height: 36)
                                    .overlay(
                                        Image(systemName: item.icon)
                                            .font(.system(size: 14, weight: .semibold))
                                            .foregroundColor(item.color)
                                    )
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(item.category)
                                        .font(.system(size: 15, weight: .semibold))
                                        .foregroundColor(AppColors.blackTextColor)
                                    Text(String(format: "%.1f%%", item.percentage))
                                        .font(.system(size: 12))
                                        .foregroundColor(AppColors.grayTextColor)
                                }
                                Spacer()
                                Text(formatted(item.amount))
                                    .font(.system(size: 15, weight: .semibold))
                                    .foregroundColor(AppColors.blackTextColor)
                            }
                            .padding(.horizontal, 20)
                            .padding(.vertical, 14)

                            if index < breakdown.count - 1 {
                                Divider().padding(.leading, 82)
                            }
                        }
                    }
                    .background(AppColors.whiteFrameColor)
                    .clipShape(RoundedRectangle(cornerRadius: 22))
                    .overlay(RoundedRectangle(cornerRadius: 22).stroke(AppColors.lightGrayFrameColor, lineWidth: 1))
                    .padding(.horizontal, 20)
                }
                .padding(.top, 16)
                .padding(.bottom, 32)
            }
            .background(Color(.systemBackground))
            .navigationTitle("Все категории")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
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
