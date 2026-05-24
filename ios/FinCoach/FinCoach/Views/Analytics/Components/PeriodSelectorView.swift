//
//  PeriodSelectorView.swift
//  FinCoach
//

import SwiftUI

struct PeriodSelectorView: View {
    @Binding var selected: AnalyticsPeriod

    var body: some View {
        HStack(spacing: 4) {
            ForEach(AnalyticsPeriod.allCases) { period in
                Button {
                    withAnimation(.easeInOut(duration: 0.2)) { selected = period }
                } label: {
                    Text(LocalizedStringKey(period.rawValue))
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(selected == period ? AppColors.blackTextColor : AppColors.grayTextColor)
                        .frame(maxWidth: .infinity)
                        .frame(height: 34)
                        .background(selected == period ? Color.white : Color.clear)
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                }
            }
        }
        .padding(4)
        .background(AppColors.lightGreenFrameColor)
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }
}
