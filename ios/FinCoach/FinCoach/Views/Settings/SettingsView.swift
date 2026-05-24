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
    @AppStorage("appColorScheme") private var colorScheme: String = "system"
    @AppStorage("appLanguage") private var language: String = "ru"
    @State private var showSignOutConfirmation = false
    
    var body: some View {
        NavigationStack {
            List {
                Section {
                    HStack(spacing: 12) {
                        Circle()
                            .fill(AppColors.lightGreenFrameColor)
                            .frame(width: 60, height: 60)
                            .overlay(
                                Text(getInitials())
                                    .font(.title2)
                                    .fontWeight(.semibold)
                                    .foregroundColor(.white)
                            )
                        
                        VStack(alignment: .leading, spacing: 4) {
                            Text(authViewModel.currentUser?.email ?? "")
                                .font(.headline)
                                .foregroundColor(AppColors.grayTextColor)
                        }
                    }
                    .padding(.vertical, 8)
                } header: {
                    Text("Профиль")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(AppColors.blackTextColor)
                        .textCase(nil)
                }
                
                Section {
                    HStack {
                        Image(systemName: "moon.fill")
                            .foregroundColor(AppColors.lightGreenFrameColor)
                            .frame(width: 28)
                        Picker("Тема", selection: $colorScheme) {
                            Text("Системная").tag("system")
                            Text("Светлая").tag("light")
                            Text("Тёмная").tag("dark")
                        }
                        .pickerStyle(.menu)
                        .tint(AppColors.grayTextColor)
                    }

                    HStack {
                        Image(systemName: "globe")
                            .foregroundColor(AppColors.lightGreenFrameColor)
                            .frame(width: 28)
                        Picker("Язык", selection: $language) {
                            Text("Русский").tag("ru")
                            Text("English").tag("en")
                        }
                        .pickerStyle(.menu)
                        .tint(AppColors.grayTextColor)
                    }
                } header: {
                    Text("Приложение")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(AppColors.blackTextColor)
                        .textCase(nil)
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
            .background(AppColors.backgroundColor)
            .navigationTitle("Настройки")
            .navigationBarTitleDisplayMode(.inline)
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
