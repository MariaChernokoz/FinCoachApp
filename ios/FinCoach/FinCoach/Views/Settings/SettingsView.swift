//
//  SettingsView.swift
//  FinCoach
//
//  Created by Chernokoz on 09.02.2026.
//

import SwiftUI
import FirebaseAuth

struct SettingsView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @State private var showSignOutConfirmation = false
    
    var body: some View {
        NavigationStack {
            List {
                Section {
                    HStack(spacing: 12) {
                        Circle()
                            .fill(
                                LinearGradient(
                                    colors: [AppColors.lightGreenFrameColor, AppColors.darkGreenFrameColor],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                            )
                            .frame(width: 60, height: 60)
                            .overlay(
                                Text(getInitials())
                                    .font(.title2)
                                    .fontWeight(.semibold)
                                    .foregroundColor(.white)
                            )
                        
                        VStack(alignment: .leading, spacing: 4) {
                            Text(authViewModel.currentUser?.displayName ?? "Пользователь")
                                .font(.headline)
                            
                            Text(authViewModel.currentUser?.email ?? "")
                                .font(.subheadline)
                                .foregroundColor(.gray)
                        }
                    }
                    .padding(.vertical, 8)
                } header: {
                    Text("ПРОФИЛЬ")
                }
                
                Section {
                    Button(role: .destructive) {
                        showSignOutConfirmation = true
                    } label: {
                        HStack {
                            Image(systemName: "rectangle.portrait.and.arrow.right")
                            Text("Выйти из аккаунта")
                        }
                    }
                }
            }
            .scrollContentBackground(.hidden)
            .background(AppColors.backgroundGray)
            .alert("Вы уверены, что хотите выйти?", isPresented: $showSignOutConfirmation) {
                Button("Выйти", role: .destructive) {
                    authViewModel.signOut()
                }
                Button("Отмена", role: .cancel) { }
            }
        }
    }
    
    private func getInitials() -> String {
        guard let name = authViewModel.currentUser?.displayName else {
            return "👤"
        }
        
        let components = name.components(separatedBy: " ")
        let initials = components.compactMap { $0.first }.map { String($0) }
        return initials.prefix(2).joined().uppercased()
    }
}

#Preview {
    SettingsView()
        .environmentObject(AuthViewModel())
}
