//
//  TabBarView.swift
//  FinCoach
//
//  Created by Chernokoz on 09.02.2026.
//

import SwiftUI

struct TabBarView: View {
    var body: some View {
        TabView {
            AnalyticsView()
                .tabItem {
                    AppIcons.analyticsIcon
                }
            
            TransactionsView()
                .tabItem {
                    AppIcons.transactionsIcon
                }
            
            AIAssistantView()
                .tabItem {
                    AppIcons.AIAssistantIcon
                }
            
            GoalsView()
                .tabItem {
                    AppIcons.goalsIcon
                }
            
            SettingsView()
                .tabItem {
                    AppIcons.settingsIcon
                }
        }
    }
}

#Preview {
    TabBarView()
}
