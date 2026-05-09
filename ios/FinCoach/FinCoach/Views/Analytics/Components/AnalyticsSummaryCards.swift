//
//  AnalyticsSummaryCards.swift
//  FinCoach
//

import SwiftUI
import Combine

struct AnalyticsSummaryCards: View {
    @ObservedObject var viewModel: AnalyticsViewModel

    var body: some View {
        VStack(spacing: 10) {
            HStack(spacing: 10) {
                card(title: "Доходы",     amount: viewModel.periodIncome,        color: AppColors.lightGreenFrameColor)
                card(title: "Расходы",    amount: viewModel.periodExpense,        color: AppColors.blackTextColor)
                card(title: "Ср. в день", amount: viewModel.averageDailyExpense,  color: AppColors.grayTextColor)
            }

            if let rate = viewModel.savingsRate {
                savingsBanner(rate: rate)
            }
        }
    }

    private func card(title: String, amount: Double, color: Color) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.system(size: 12, weight: .regular))
                .foregroundColor(AppColors.grayTextColor)
                .lineLimit(1)
            Text(formatted(amount))
                .font(.system(size: 15, weight: .bold))
                .foregroundColor(color)
                .lineLimit(1)
                .minimumScaleFactor(0.6)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(AppColors.whiteFrameColor)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(AppColors.lightGrayFrameColor, lineWidth: 1))
        .cornerRadius(16)
    }

    private func savingsBanner(rate: Double) -> some View {
        let positive = rate > 0
        return HStack(spacing: 8) {
            Image(systemName: positive ? "checkmark.circle.fill" : "exclamationmark.circle.fill")
                .font(.system(size: 14))
                .foregroundColor(positive ? AppColors.lightGreenFrameColor : .red)
            Text(positive
                 ? String(format: "Вы сэкономили %.0f%% дохода", rate)
                 : String(format: "Расходы превысили доходы на %.0f%%", abs(rate)))
                .font(.system(size: 13, weight: .medium))
                .foregroundColor(positive ? AppColors.darkGreenFrameColor : .red)
            Spacer()
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .background((positive ? AppColors.lightGreenFrameColor : Color.red).opacity(0.08))
        .cornerRadius(12)
    }

    private func formatted(_ amount: Double) -> String {
        let f = NumberFormatter()
        f.numberStyle = .currency
        f.currencyCode = "RUB"
        f.locale = Locale(identifier: "ru_RU")
        f.maximumFractionDigits = 0
        return f.string(from: NSNumber(value: amount)) ?? "\(Int(amount))"
    }
}
