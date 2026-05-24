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
                Spacer().frame(height: 32)
                Text(currency(amount))
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(AppColors.blackTextColor)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
                    .padding(.bottom, -4)
                Text(LocalizedStringKey(title))
                    .font(.system(size: 14, weight: .regular))
                    .foregroundColor(AppColors.grayTextColor)
                    .padding(.bottom, -4)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(AppColors.backgroundColor)
            .cornerRadius(16)
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(AppColors.lightGrayFrameColor, lineWidth: 1))
            .padding(.top, 20)
            .shadow(color: Color.black.opacity(0.1), radius: 10, x: 0, y: 10)

            Image(imageName)
                .renderingMode(.original)
                .resizable()
                .scaledToFit()
                .frame(width: 76, height: 76)
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

#Preview {
    HStack(spacing: 16) {
        OverflowMetricCard(title: "Расходы", amount: 45200, imageName: "money")
        OverflowMetricCard(title: "Доходы", amount: 120000, imageName: "pig")
    }
    .padding()
    .background(AppColors.lightGreenFrameColor)
}
