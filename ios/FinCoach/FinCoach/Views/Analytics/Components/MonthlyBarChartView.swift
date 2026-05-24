//
//  MonthlyBarChartView.swift
//  FinCoach
//

import SwiftUI
import Charts

struct MonthlyBarChartView: View {
    let points: [MonthlyPoint]

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Динамика по месяцам")
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(AppColors.blackTextColor)

            Chart {
                ForEach(points) { point in
                    BarMark(
                        x: .value("Месяц", point.label),
                        y: .value("Доходы", point.income)
                    )
                    .foregroundStyle(AppColors.lightGreenFrameColor)
                    .position(by: .value("Тип", "Доходы"))
                    .cornerRadius(4)

                    BarMark(
                        x: .value("Месяц", point.label),
                        y: .value("Расходы", point.expense)
                    )
                    .foregroundStyle(AppColors.darkGrayTextColor)
                    .position(by: .value("Тип", "Расходы"))
                    .cornerRadius(4)
                }
            }
            .frame(height: 180)
            .chartXAxis {
                AxisMarks { _ in
                    AxisValueLabel()
                        .font(.system(size: 11))
                        .foregroundStyle(AppColors.grayTextColor)
                }
            }
            .chartYAxis {
                AxisMarks { _ in
                    AxisGridLine(stroke: StrokeStyle(lineWidth: 0.5, dash: [4]))
                        .foregroundStyle(AppColors.lightGrayFrameColor)
                    AxisValueLabel()
                        .font(.system(size: 11))
                        .foregroundStyle(AppColors.grayTextColor)
                }
            }

            HStack(spacing: 16) {
                legendDot(color: AppColors.lightGreenFrameColor, label: "Доходы")
                legendDot(color: AppColors.darkGrayTextColor,    label: "Расходы")
            }
        }
        .padding(20)
        .background(AppColors.whiteFrameColor)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(AppColors.lightGrayFrameColor, lineWidth: 1))
        .cornerRadius(16)
    }

    private func legendDot(color: Color, label: String) -> some View {
        HStack(spacing: 6) {
            RoundedRectangle(cornerRadius: 3)
                .fill(color)
                .frame(width: 14, height: 10)
            Text(label)
                .font(.system(size: 13))
                .foregroundColor(AppColors.grayTextColor)
        }
    }
}
