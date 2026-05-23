//
//  GoalCardView.swift
//  FinCoach
//

import SwiftUI

struct GoalCardView: View {
    let goal: Goal
    let onEdit: () -> Void
    let onDelete: () -> Void

    var body: some View {
        HStack(spacing: 16) {
            circularProgress

            VStack(alignment: .leading, spacing: 5) {
                HStack(alignment: .top) {
                    Text(goal.title)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(AppColors.blackTextColor)
                        .lineLimit(2)
                    Spacer()
                    if goal.isCompleted {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.system(size: 18))
                            .foregroundColor(AppColors.lightGreenFrameColor)
                    }
                }

                Text("\(formatted(goal.savedAmount)) из \(formatted(goal.targetAmount))")
                    .font(.system(size: 13))
                    .foregroundColor(AppColors.grayTextColor)

                if let deadlineMs = goal.deadline {
                    deadlineLabel(deadlineMs)
                }

                if let monthly = monthlyAmount {
                    Text("≈ \(formatted(monthly)) / мес.")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundColor(AppColors.lightGreenFrameColor)
                }
            }
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 16)
        .contentShape(Rectangle())
        .onTapGesture { onEdit() }
        .swipeToDelete { onDelete() }
    }

    private var circularProgress: some View {
        ZStack {
            Circle()
                .stroke(AppColors.lightGrayFrameColor, lineWidth: 5)
                .frame(width: 56, height: 56)

            Circle()
                .trim(from: 0, to: CGFloat(goal.progress))
                .stroke(
                    goal.isCompleted ? AppColors.lightGreenFrameColor : AppColors.lightGreenFrameColor,
                    style: StrokeStyle(lineWidth: 5, lineCap: .round)
                )
                .frame(width: 56, height: 56)
                .rotationEffect(.degrees(-90))
                .animation(.easeOut(duration: 0.5), value: goal.progress)

            Text(String(format: "%.0f%%", goal.progress * 100))
                .font(.system(size: 11, weight: .bold))
                .foregroundColor(AppColors.blackTextColor)
        }
        .frame(width: 56, height: 56)
    }

    private func deadlineLabel(_ deadlineMs: Int64) -> some View {
        let date = Date(timeIntervalSince1970: Double(deadlineMs) / 1000)
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "ru_RU")
        formatter.dateFormat = "d MMM yyyy"
        return HStack(spacing: 4) {
            Image(systemName: "calendar")
                .font(.system(size: 10))
                .foregroundColor(AppColors.grayTextColor)
            Text(formatter.string(from: date))
                .font(.system(size: 11))
                .foregroundColor(AppColors.grayTextColor)
        }
    }

    private var monthlyAmount: Double? {
        guard let deadlineMs = goal.deadline,
              !goal.isCompleted,
              goal.savedAmount < goal.targetAmount else { return nil }
        let deadlineDate = Date(timeIntervalSince1970: Double(deadlineMs) / 1000)
        let now = Date()
        guard deadlineDate > now else { return nil }
        let months = Calendar.current.dateComponents([.month], from: now, to: deadlineDate).month ?? 0
        let monthsRemaining = max(1, months)
        return (goal.targetAmount - goal.savedAmount) / Double(monthsRemaining)
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
