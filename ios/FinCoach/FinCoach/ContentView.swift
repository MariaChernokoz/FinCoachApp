//
//  ContentView.swift
//  FinCoach
//
//  Created by Chernokoz on 05.02.2026.
//

import SwiftUI
import FirebaseAuth

struct ContentView: View {
    @StateObject private var authViewModel = AuthViewModel()
    @StateObject private var navigationState = AppNavigationState()
    @AppStorage("appColorScheme") private var colorScheme: String = "system"
    @AppStorage("appLanguage") private var language: String = "ru"

    var body: some View {
        Group {
            if authViewModel.isAuthenticated {
                TabBarView()
                    .environmentObject(authViewModel)
                    .environmentObject(navigationState)
            } else {
                AuthView()
                    .environmentObject(authViewModel)
            }
        }
        .environment(\.locale, Locale(identifier: language))
        .preferredColorScheme(resolvedScheme)
        .onAppear {
            authViewModel.checkAuthStatus()
        }
    }

    private var resolvedScheme: ColorScheme? {
        switch colorScheme {
        case "light": return .light
        case "dark":  return .dark
        default:      return nil
        }
    }
}

#Preview {
    ContentView()
}
