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
    
    var body: some View {
        Group {
            if authViewModel.isAuthenticated {
                TabBarView()
                    .environmentObject(authViewModel)
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
