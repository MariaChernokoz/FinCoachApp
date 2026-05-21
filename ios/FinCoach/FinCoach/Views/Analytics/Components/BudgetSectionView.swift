//
//  BudgetSectionView.swift
//  FinCoach
//

import SwiftUI

struct BudgetSectionView: View {
    @ObservedObject var viewModel: AnalyticsViewModel
    let onAdd: () -> Void
    let onEdit: (Budget) -> Void
    let onDelete: (Budget) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                Text("Бюджеты")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(AppColors.blackTextColor)
                Spacer()
                Button(action: onAdd) {
                    HStack(spacing: 4) {
                        Image(systemName: "plus")
                        Text("Добавить")
                    }
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(AppColors.lightGreenFrameColor)
                }
            }

            if viewModel.budgets.isEmpty {
                emptyView
            } else {
                VStack(spacing: 0) {
                    ForEach(viewModel.budgets) { budget in
                        let cat = viewModel.category(for: budget)
                        BudgetCardView(
                            budget: budget,
                            categoryTitle: cat?.title ?? budget.categoryId,
                            categoryIcon: cat?.icon ?? "tag",
                            spent: viewModel.currentMonthSpent(for: budget),
                            fraction: viewModel.spentFraction(for: budget),
                            onEdit: { onEdit(budget) },
                            onDelete: { onDelete(budget) }
                        )
                        if budget.id != viewModel.budgets.last?.id {
                            Divider().padding(.leading, 82)
                        }
                    }
                }
                .background(AppColors.whiteFrameColor)
                .clipShape(RoundedRectangle(cornerRadius: 22))
                .overlay(RoundedRectangle(cornerRadius: 22).stroke(AppColors.lightGrayFrameColor, lineWidth: 1))
            }
        }
    }

    private var emptyView: some View {
        VStack(spacing: 12) {
            Image(systemName: "list.bullet.rectangle")
                .font(.system(size: 34, weight: .light))
                .foregroundColor(AppColors.lightGreenFrameColor.opacity(0.7))
            Text("Бюджеты не заданы")
                .font(.system(size: 16, weight: .medium))
                .foregroundColor(AppColors.grayTextColor)
            Text("Добавьте лимит по категории,\nчтобы контролировать траты")
                .font(.system(size: 13))
                .foregroundColor(AppColors.grayTextColor)
                .multilineTextAlignment(.center)
            Button(action: onAdd) {
                Label("Добавить бюджет", systemImage: "plus")
                    .font(.headline)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 48)
                    .background(AppColors.lightGreenFrameColor)
                    .cornerRadius(14)
            }
        }
        .padding(24)
        .frame(maxWidth: .infinity)
        .background(AppColors.whiteFrameColor)
        .overlay(RoundedRectangle(cornerRadius: 22).stroke(AppColors.lightGrayFrameColor, lineWidth: 1))
        .cornerRadius(22)
    }
}
