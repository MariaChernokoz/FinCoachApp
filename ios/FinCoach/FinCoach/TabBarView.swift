//
//  TabBarView.swift
//  FinCoach
//
//  Created by Chernokoz on 09.02.2026.
//

import SwiftUI

struct TabBarView: View {
    @StateObject private var networkMonitor = NetworkMonitor()

    var body: some View {
        ZStack(alignment: .top) {
            TabView {
                AnalyticsView()
                    .tabItem { AppIcons.analyticsIcon }

                TransactionsView()
                    .tabItem { AppIcons.transactionsIcon }

                AIAssistantView()
                    .tabItem { AppIcons.AIAssistantIcon }

                GoalsView()
                    .tabItem { AppIcons.goalsIcon }

                SettingsView()
                    .tabItem { AppIcons.settingsIcon }
            }

            if !networkMonitor.isConnected {
                VStack {
                    OfflineBannerView()
                        .padding(.top, 12)
                    Spacer()
                }
                .transition(.move(edge: .top).combined(with: .opacity))
                .zIndex(1)
            }
        }
        .animation(.easeInOut(duration: 0.35), value: networkMonitor.isConnected)
    }
}

#Preview {
    TabBarView()
}
