//
//  OverflowMetricCard.swift
//  FinCoach
//
//  Created by Chernokoz on 08.05.2026.
//

import SwiftUI

struct OverflowMetricCard: View {
    let title: String
    let amount: Double
    let imageName: String

    var body: some View {
        ZStack(alignment: .top) {
            VStack(spacing: 4) {
                Spacer().frame(height: 52)
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
            .padding(.top, 20)

            Image(imageName)
                .renderingMode(.original)
                .resizable()
                .scaledToFit()
                .frame(width: 96, height: 96)
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
