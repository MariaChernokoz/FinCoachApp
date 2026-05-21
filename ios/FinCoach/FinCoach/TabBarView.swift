//
//  TabBarView.swift
//  FinCoach
//
//  Created by Chernokoz on 09.02.2026.
//

import SwiftUI

struct TabBarView: View {
    @StateObject private var networkMonitor = NetworkMonitor()
    @EnvironmentObject private var navigationState: AppNavigationState

    var body: some View {
        ZStack(alignment: .top) {
            TabView(selection: $navigationState.selectedTab) {
                AnalyticsView()
                    .tabItem { AppIcons.analyticsIcon }
                    .tag(0)

                TransactionsView()
                    .tabItem { AppIcons.transactionsIcon }
                    .tag(1)

                AIAssistantView()
                    .tabItem { AppIcons.AIAssistantIcon }
                    .tag(2)

                GoalsView()
                    .tabItem { AppIcons.goalsIcon }
                    .tag(3)

                SettingsView()
                    .tabItem { AppIcons.settingsIcon }
                    .tag(4)
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
