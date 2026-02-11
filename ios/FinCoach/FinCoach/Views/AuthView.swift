//
//  AuthView.swift
//  FinCoach
//
//  Created by Chernokoz on 09.02.2026.
//

import SwiftUI
import FirebaseAuth

struct AuthView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @State private var isLoginMode = true
    @State private var email = ""
    @State private var password = ""
    @State private var name = ""
    @State private var showForgotPassword = false
    
    var body: some View {
        ZStack {
            AppColors.linearGreenGradient
            .ignoresSafeArea()
            
            ScrollView {
                VStack {
                    Spacer(minLength: 40)
                    
                    VStack(spacing: 16) {
                        Text("Welcome to FinCoach AI!")
                            .font(.system(size: 30, weight: .bold))
                            .foregroundColor(AppColors.whiteTextColor)
                        
                        Text("Your personal finance coach")
                            .font(.subheadline)
                            .foregroundColor(AppColors.whiteTextColor.opacity(0.9))
                        Image("FinCoachLogoWhite")
                    }
                    .padding(.bottom, 20)
                    
                    VStack(spacing: 16) {
                        Picker("Auth Mode", selection: $isLoginMode) {
                            Text("Вход").tag(true)
                            Text("Регистрация").tag(false)
                        }
                        .pickerStyle(.segmented)
                        .padding(.horizontal, 20)
                        .padding(.top, 16)
                        
                        VStack(spacing: 16) {
                            if !isLoginMode {
                                AuthTextField(
                                    icon: "person.fill",
                                    placeholder: "Имя",
                                    text: $name
                                )
                            }
                            
                            AuthTextField(
                                icon: "envelope.fill",
                                placeholder: "Email",
                                text: $email,
                                keyboardType: .emailAddress,
                                autocapitalization: .never
                            )
                            
                            AuthTextField(
                                icon: "lock.fill",
                                placeholder: "Пароль",
                                text: $password,
                                isSecure: true
                            )
                        }
                        .padding(.horizontal, 20)
                        
                        if isLoginMode {
                            Button {
                                showForgotPassword = true
                            } label: {
                                Text("Забыли пароль?")
                                    .font(.subheadline)
                                    .foregroundColor(AppColors.grayTextColor)
                            }
                            .padding(.trailing, 20)
                            .frame(maxWidth: .infinity, alignment: .trailing)
                            .padding(.top, -8)
                        }
                        
                        Button {
                            handleAuthAction()
                        } label: {
                            ZStack {
                                if authViewModel.isLoading {
                                    ProgressView()
                                        .progressViewStyle(CircularProgressViewStyle(tint: AppColors.whiteFrameColor))
                                } else {
                                    Text(isLoginMode ? "ВОЙТИ" : "ЗАРЕГИСТРИРОВАТЬСЯ")
                                        .font(.headline)
                                        .foregroundColor(AppColors.whiteFrameColor)
                                }
                            }
                            .frame(maxWidth: .infinity)
                            .frame(height: 50)
                            .background(AppColors.linearGreenGradient)
                            .cornerRadius(16)
                        }
                        .disabled(authViewModel.isLoading)
                        .padding(.horizontal, 20)
                        .padding(.top, 8)
                        
                        HStack {
                            Rectangle()
                                .fill(Color.gray.opacity(0.3))
                                .frame(height: 1)
                            
                            Text("или")
                                .font(.subheadline)
                                .foregroundColor(.gray)
                                .padding(.horizontal, 8)
                            
                            Rectangle()
                                .fill(Color.gray.opacity(0.3))
                                .frame(height: 1)
                        }
                        .padding(.horizontal, 20)
                        .padding(.vertical, 8)
                        
                        VStack(spacing: 12) {
                            SocialLoginButton(
                                icon: "g.circle.fill",
                                title: "Войти через Google",
                                backgroundColor: AppColors.whiteFrameColor,
                                foregroundColor: AppColors.blackTextColor
                            ) {
                                print("Google Sign-In tapped")
                            }
                            
                            SocialLoginButton(
                                icon: "apple.logo",
                                title: "Войти через Apple",
                                backgroundColor: AppColors.blackFrameColor,
                                foregroundColor: AppColors.whiteTextColor
                            ) {
                                print("Apple Sign-In tapped")
                            }
                        }
                        .padding(.horizontal, 20)
                        .padding(.bottom, 20)
                    }
                    .background(AppColors.whiteFrameColor)
                    .cornerRadius(20)
                    .shadow(color: AppColors.blackFrameColor.opacity(0.1), radius: 10, x: 0, y: 5)
                    .padding(.horizontal, 20)
                    
                    Spacer(minLength: 40)
                }
            }
        }
        .alert("Уведомление", isPresented: $authViewModel.showError) {
            Button("OK", role: .cancel) {
                authViewModel.showError = false
            }
        } message: {
            Text(authViewModel.errorMessage ?? "Произошла ошибка")
        }
        .sheet(isPresented: $showForgotPassword) {
            ForgotPasswordView()
                .environmentObject(authViewModel)
        }
    }
    
    // MARK: - Actions
    private func handleAuthAction() {
        UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
        
        if isLoginMode {
            authViewModel.signIn(email: email, password: password)
        } else {
            authViewModel.signUp(email: email, password: password, name: name)
        }
    }
}


// MARK: - Custom AuthTextField
struct AuthTextField: View {
    let icon: String
    let placeholder: String
    @Binding var text: String
    var keyboardType: UIKeyboardType = .default
    var autocapitalization: TextInputAutocapitalization = .sentences
    var isSecure: Bool = false
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .foregroundColor(AppColors.grayTextColor)
                .frame(width: 24)
            
            if isSecure {
                SecureField(placeholder, text: $text)
                    .textInputAutocapitalization(.never)
            } else {
                TextField(placeholder, text: $text)
                    .keyboardType(keyboardType)
                    .textInputAutocapitalization(autocapitalization)
            }
        }
        .padding()
        .background(AppColors.lightGrayFrameColor)
        .cornerRadius(16)
    }
}

// MARK: - Social Login Button
struct SocialLoginButton: View {
    let icon: String
    let title: String
    let backgroundColor: Color
    let foregroundColor: Color
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack {
                Image(systemName: icon)
                    .font(.title3)
                
                Text(title)
                    .font(.subheadline)
                    .fontWeight(.medium)
            }
            .foregroundColor(foregroundColor)
            .frame(maxWidth: .infinity)
            .frame(height: 50)
            .background(backgroundColor)
            .cornerRadius(12)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(AppColors.grayTextColor.opacity(0.3), lineWidth: 1)
            )
        }
    }
}

// MARK: - Forgot Password View
struct ForgotPasswordView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @Environment(\.dismiss) var dismiss
    
    @State private var email = ""
    
    var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                Text("Введите ваш email и мы отправим инструкцию по восстановлению пароля")
                    .font(.subheadline)
                    .foregroundColor(AppColors.grayTextColor)
                    .multilineTextAlignment(.center)
                    .padding()
                
                AuthTextField(
                    icon: "envelope.fill",
                    placeholder: "Email",
                    text: $email,
                    keyboardType: .emailAddress,
                    autocapitalization: .never
                )
                .padding(.horizontal)
                
                Button {
                    authViewModel.resetPassword(email: email)
                    dismiss()
                } label: {
                    Text("ОТПРАВИТЬ")
                        .font(.headline)
                        .foregroundColor(AppColors.whiteTextColor)
                        .frame(maxWidth: .infinity)
                        .frame(height: 50)
                        .background(AppColors.linearGreenGradient)
                        .cornerRadius(12)
                }
                .disabled(email.isEmpty)
                .padding(.horizontal)
                
                Spacer()
            }
            .navigationTitle("Восстановление пароля")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Закрыть") {
                        dismiss()
                    }
                }
            }
        }
    }
}

#Preview {
    AuthView()
        .environmentObject(AuthViewModel())
}

