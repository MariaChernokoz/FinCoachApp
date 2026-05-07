//
//  TransactionRowView.swift
//  FinCoach
//
//  Created by Chernokoz on 08.05.2026.
//

import SwiftUI

struct TransactionRowView: View {
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
