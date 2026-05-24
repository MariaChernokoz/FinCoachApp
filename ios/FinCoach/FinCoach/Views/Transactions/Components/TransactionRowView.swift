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
                .fill(AppColors.lightGreenFrameColor.opacity(0.7))
                .frame(width: 40, height: 40)
                .overlay(
                    Image(systemName: iconName)
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(AppColors.darkGrayFrameColor)
                )

            VStack(alignment: .leading, spacing: 7) {
                Text(transaction.title)
                    .font(.system(size: 16, weight: .semibold))
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
                    .font(.system(size: 16, weight: .semibold))
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

    private var emoji: String {
        switch iconName {
        case "cart":                        return "🛒"
        case "car":                         return "🚗"
        case "cup.and.saucer":              return "☕️"
        case "creditcard":                  return "💳"
        case "wallet.pass":                 return "💰"
        case "house", "house.fill":         return "🏠"
        case "bag":                         return "🛍️"
        case "fork.knife":                  return "🍽️"
        case "pawprint":                    return "🐾"
        case "heart":                       return "❤️"
        case "gift":                        return "🎁"
        case "briefcase":                   return "💼"
        case "laptopcomputer",
             "desktopcomputer":             return "💻"
        case "film":                        return "🎬"
        case "bus", "tram":                 return "🚌"
        case "person.2":                    return "👪"
        case "tshirt":                      return "👕"
        case "chart.line.uptrend.xyaxis":   return "📈"
        case "percent":                     return "💹"
        case "airplane":                    return "✈️"
        case "bolt":                        return "⚡️"
        case "sparkles":                    return "✨"
        case "wrench.and.screwdriver":      return "🔧"
        case "graduationcap":               return "🎓"
        case "figure.run":                  return "🏃"
        case "cross.case":                  return "💊"
        case "tag":                         return "🏷️"
        case "ellipsis.circle":             return "🔘"
        default:                            return transaction.isIncome ? "💰" : "💳"
        }
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
