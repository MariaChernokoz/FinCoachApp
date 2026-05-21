//
//  OfflineBannerView.swift
//  FinCoach
//

import SwiftUI

struct OfflineBannerView: View {
    var body: some View {
        HStack(spacing: 6) {
            Image(systemName: "wifi.slash")
                .font(.system(size: 12, weight: .semibold))
            Text("Нет интернета")
                .font(.system(size: 13, weight: .semibold))
        }
        .foregroundColor(.white)
        .padding(.horizontal, 14)
        .padding(.vertical, 7)
        .background(Color(red: 0.85, green: 0.25, blue: 0.25).opacity(0.95))
        .clipShape(Capsule())
        .shadow(color: .black.opacity(0.18), radius: 6, x: 0, y: 3)
    }
}

#Preview {
    OfflineBannerView()
        .padding()
}
