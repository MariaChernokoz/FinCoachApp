//
//  EmptyTransactionsView.swift
//  FinCoach
//
//  Created by Chernokoz on 08.05.2026.
//

import SwiftUI

struct EmptyTransactionsView: View {
    let addAction: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Circle()
                .fill(AppColors.lightGreenFrameColor.opacity(0.14))
                .frame(width: 72, height: 72)
                .overlay(
                    Image(systemName: "creditcard")
                        .font(.system(size: 28, weight: .semibold))
                        .foregroundColor(AppColors.lightGreenFrameColor)
                )

            VStack(spacing: 6) {
                Text("Транзакций пока нет")
                    .font(.system(size: 20, weight: .semibold))
                Text("Добавьте первый доход или расход")
                    .font(.subheadline)
                    .foregroundColor(AppColors.grayTextColor)
            }

            Button {
                addAction()
            } label: {
                Label("Добавить", systemImage: "plus")
                    .font(.headline)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 50)
                    .background(AppColors.lightGreenFrameColor)
                    .cornerRadius(16)
            }
        }
        .padding(24)
        .frame(maxWidth: .infinity)
        .background(AppColors.whiteFrameColor)
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(AppColors.lightGrayFrameColor, lineWidth: 1)
        )
        .cornerRadius(16)
    }
}
