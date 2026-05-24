//
//  AnalyticsSummaryCards.swift
//  FinCoach
//

import SwiftUI
import Combine

struct AnalyticsSummaryCards: View {
    @ObservedObject var viewModel: AnalyticsViewModel
    @Binding var period: AnalyticsPeriod

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {

            // Period label
            Text(periodLabel)
                .font(.system(size: 12, weight: .semibold))
                .foregroundColor(AppColors.grayTextColor)
                .padding(.horizontal, 20)
                .padding(.top, 18)
                .padding(.bottom, 14)

            // Three metrics
            HStack(spacing: 0) {
                metricColumn(title: "Доходы",         amount: viewModel.periodIncome)
                columnDivider
                metricColumn(title: "Расходы",        amount: viewModel.periodExpense)
                columnDivider
                metricColumn(title: "Среднее в день", amount: viewModel.averageDailyExpense)
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 16)

            // Period selector inside card at the bottom
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    ForEach(AnalyticsPeriod.allCases) { p in
                        Button {
                            withAnimation(.easeInOut(duration: 0.2)) { period = p }
                        } label: {
                            Text(p.rawValue)
                                .font(.system(size: 13, weight: .medium))
                                .foregroundColor(period == p
                                    ? AppColors.blackFrameColor
                                    : AppColors.grayTextColor)
                                .padding(.horizontal, 16)
                                .frame(height: 30)
                                .background(period == p ? Color.white : Color.clear)
                                .cornerRadius(10)
                        }
                    }
                }
                .padding(.horizontal, 12)
            }
            .padding(.top, 2)
            .padding(.bottom, 12)
        }
        .background(AppColors.lightGreenFrameColor)
        .cornerRadius(22)
    }

    private func card(title: String, amount: Double, color: Color) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(LocalizedStringKey(title))
                .font(.system(size: 12, weight: .regular))
                .foregroundColor(AppColors.grayTextColor)
                .lineLimit(1)
            HStack(alignment: .bottom, spacing: 3) {
                Text(formattedNumber(amount))
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(AppColors.blackTextColor)
                    .minimumScaleFactor(0.55)
                    .lineLimit(1)
                Text("₽")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(AppColors.blackTextColor)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var columnDivider: some View {
        Rectangle()
            .fill(AppColors.grayTextColor.opacity(0.4))
            .frame(width: 1, height: 44)
            .padding(.horizontal, 6)
    }

    // MARK: - Helpers

    private var periodLabel: String {
        let fmt = DateFormatter()
        fmt.locale = Locale(identifier: "ru_RU")
        let now = Date()
        switch period {
        case .week:
            let (start, end) = period.dateRange
            fmt.dateFormat = "d MMM"
            return "\(fmt.string(from: start)) – \(fmt.string(from: end))".uppercased()
        case .month:
            fmt.dateFormat = "LLLL yyyy"
            return fmt.string(from: now).uppercased()
        case .threeMonths:
            let (start, _) = period.dateRange
            fmt.dateFormat = "LLL"
            return "\(fmt.string(from: start).uppercased()) – \(fmt.string(from: now).uppercased())"
        case .year:
            let (start, _) = period.dateRange
            fmt.dateFormat = "LLL yyyy"
            return "\(fmt.string(from: start).uppercased()) – \(fmt.string(from: now).uppercased())"
        }
    }

    private func formattedNumber(_ amount: Double) -> String {
        let f = NumberFormatter()
        f.numberStyle = .decimal
        f.groupingSeparator = " "
        f.groupingSize = 3
        f.maximumFractionDigits = 0
        return f.string(from: NSNumber(value: amount)) ?? "\(Int(amount))"
    }
}
