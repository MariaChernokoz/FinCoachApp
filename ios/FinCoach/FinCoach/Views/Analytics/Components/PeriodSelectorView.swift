//
//  PeriodSelectorView.swift
//  FinCoach
//

import SwiftUI

struct PeriodSelectorView: View {
    @Binding var selected: AnalyticsPeriod

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(AnalyticsPeriod.allCases) { period in
                    Button {
                        withAnimation(.easeInOut(duration: 0.2)) { selected = period }
                    } label: {
                        Text(period.rawValue)
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(selected == period ? .white : AppColors.grayTextColor)
                            .padding(.horizontal, 18)
                            .frame(height: 36)
                            .background(selected == period ? AppColors.lightGreenFrameColor : AppColors.whiteFrameColor)
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(
                                        selected == period ? AppColors.lightGreenFrameColor : AppColors.lightGrayFrameColor,
                                        lineWidth: 1
                                    )
                            )
                            .cornerRadius(12)
                    }
                }
            }
        }
    }
}
