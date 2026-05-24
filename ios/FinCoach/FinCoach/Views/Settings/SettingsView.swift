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
    @State private var showThemePicker = false
    @State private var showLanguagePicker = false

    private var themeLabel: String {
        switch colorScheme {
        case "light": return "Светлая"
        case "dark":  return "Тёмная"
        default:      return "Системная"
        }
    }

    private var languageLabel: String {
        language == "en" ? "English" : "Русский"
    }
    
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
                    Button {
                        showThemePicker = true
                    } label: {
                        HStack {
                            Image(systemName: "moon.fill")
                                .foregroundColor(AppColors.lightGreenFrameColor)
                                .frame(width: 28)
                            Text("Тема")
                                .foregroundColor(AppColors.blackTextColor)
                            Spacer()
                            Text(themeLabel)
                                .foregroundColor(AppColors.grayTextColor)
                            Image(systemName: "chevron.right")
                                .font(.system(size: 13, weight: .medium))
                                .foregroundColor(AppColors.lightGrayFrameColor)
                        }
                    }
                    .confirmationDialog("Выберите тему", isPresented: $showThemePicker, titleVisibility: .visible) {
                        Button("Системная") { colorScheme = "system" }
                        Button("Светлая")   { colorScheme = "light" }
                        Button("Тёмная")    { colorScheme = "dark" }
                        Button("Отмена", role: .cancel) { }
                    }

                    Button {
                        showLanguagePicker = true
                    } label: {
                        HStack {
                            Image(systemName: "globe")
                                .foregroundColor(AppColors.lightGreenFrameColor)
                                .frame(width: 28)
                            Text("Язык")
                                .foregroundColor(AppColors.blackTextColor)
                            Spacer()
                            Text(languageLabel)
                                .foregroundColor(AppColors.grayTextColor)
                            Image(systemName: "chevron.right")
                                .font(.system(size: 13, weight: .medium))
                                .foregroundColor(AppColors.lightGrayFrameColor)
                        }
                    }
                    .confirmationDialog("Выберите язык", isPresented: $showLanguagePicker, titleVisibility: .visible) {
                        Button("Русский") { language = "ru" }
                        Button("English") { language = "en" }
                        Button("Отмена", role: .cancel) { }
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
