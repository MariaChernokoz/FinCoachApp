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
        .onAppear {
            authViewModel.checkAuthStatus()
        }
    }
}

#Preview {
    ContentView()
}
