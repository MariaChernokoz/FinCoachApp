//
//  AuthViewModel.swift
//  FinCoach
//
//  Created by Chernokoz on 09.02.2026.
//

import Foundation
import FirebaseAuth
import FirebaseFirestore
import Combine

final class AuthViewModel: ObservableObject {
    @Published var isAuthenticated = false
    @Published var currentUser: User?
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var showError = false
    @Published var emailVerificationSent = false
    
    private var authStateHandler: AuthStateDidChangeListenerHandle?
    private let db = Firestore.firestore()
    
    init() {
        registerAuthStateHandler()
    }
    
    deinit {
        if let handler = authStateHandler {
            Auth.auth().removeStateDidChangeListener(handler)
        }
    }
    
    // MARK: - Auth State Handling
    private func registerAuthStateHandler() {
        authStateHandler = Auth.auth().addStateDidChangeListener { [weak self] _, user in
            guard let self = self else { return }
            
            DispatchQueue.main.async {
                self.currentUser = user
                self.isAuthenticated = user != nil && (user?.isEmailVerified ?? false)
            }
        }
    }
    
    func checkAuthStatus() {
        if let user = Auth.auth().currentUser {
            user.reload { [weak self] error in
                guard let self = self else { return }
                
                DispatchQueue.main.async {
                    if let error = error {
                        print("Error reloading user: \(error.localizedDescription)")
                        self.isAuthenticated = false
                        return
                    }
                    
                    self.currentUser = user
                    self.isAuthenticated = user.isEmailVerified
                    
                    if !user.isEmailVerified {
                        self.errorMessage = "Пожалуйста, подтвердите вашу почту. Мы отправили письмо с подтверждением."
                        self.showError = true
                    }
                }
            }
        } else {
            DispatchQueue.main.async {
                self.isAuthenticated = false
                self.currentUser = nil
            }
        }
    }
    
    // MARK: - Registration
    func signUp(email: String, password: String, name: String) {
        guard !email.isEmpty, !password.isEmpty, !name.isEmpty else {
            showErrorMessage("Пожалуйста, заполните все поля")
            return
        }
        
        guard password.count >= 6 else {
            showErrorMessage("Пароль должен содержать минимум 6 символов")
            return
        }
        
        isLoading = true
        errorMessage = nil
        
        Auth.auth().createUser(withEmail: email, password: password) { [weak self] result, error in
            guard let self = self else { return }
            
            DispatchQueue.main.async {
                self.isLoading = false
                
                if let error = error {
                    self.showErrorMessage(error.localizedDescription)
                    return
                }
                
                guard let user = result?.user else {
                    self.showErrorMessage("Не удалось создать пользователя")
                    return
                }
                
                let changeRequest = user.createProfileChangeRequest()
                changeRequest.displayName = name
                changeRequest.commitChanges { error in
                    if let error = error {
                        print("Error updating profile: \(error.localizedDescription)")
                    }
                }
                
                self.createUserDocument(uid: user.uid, email: email, name: name)
                self.sendEmailVerification()
            }
        }
    }
    
    func sendEmailVerification() {
        guard let user = Auth.auth().currentUser else {
            showErrorMessage("Пользователь не найден")
            return
        }
        
        isLoading = true
        
        user.sendEmailVerification { [weak self] error in
            guard let self = self else { return }
            
            DispatchQueue.main.async {
                self.isLoading = false
                
                if let error = error {
                    self.showErrorMessage("Ошибка отправки письма: \(error.localizedDescription)")
                    return
                }
                
                self.emailVerificationSent = true
                self.errorMessage = "Письмо с подтверждением отправлено на \(user.email ?? "вашу почту"). Пожалуйста, проверьте почту и подтвердите регистрацию."
                self.showError = true
                
                try? Auth.auth().signOut()
            }
        }
    }
    
    // MARK: - Login
    func signIn(email: String, password: String) {
        guard !email.isEmpty, !password.isEmpty else {
            showErrorMessage("Пожалуйста, заполните все поля")
            return
        }
        
        isLoading = true
        errorMessage = nil
        
        Auth.auth().signIn(withEmail: email, password: password) { [weak self] result, error in
            guard let self = self else { return }
            
            DispatchQueue.main.async {
                self.isLoading = false
                
                if let error = error {
                    self.showErrorMessage(error.localizedDescription)
                    return
                }
                
                guard let user = result?.user else {
                    self.showErrorMessage("Не удалось войти")
                    return
                }
                
                if !user.isEmailVerified {
                    self.errorMessage = "Пожалуйста, подтвердите вашу почту. Мы отправили письмо с подтверждением на \(user.email ?? "вашу почту")."
                    self.showError = true
                    
                    try? Auth.auth().signOut()
                    return
                }
                
                self.currentUser = user
                self.isAuthenticated = true
            }
        }
    }
    
    // MARK: - Sign Out
    func signOut() {
        do {
            try Auth.auth().signOut()
            DispatchQueue.main.async {
                self.isAuthenticated = false
                self.currentUser = nil
            }
        } catch {
            showErrorMessage("Ошибка при выходе: \(error.localizedDescription)")
        }
    }
    
    // MARK: - Password Reset
    func resetPassword(email: String) {
        guard !email.isEmpty else {
            showErrorMessage("Введите email")
            return
        }
        
        isLoading = true
        
        Auth.auth().sendPasswordReset(withEmail: email) { [weak self] error in
            guard let self = self else { return }
            
            DispatchQueue.main.async {
                self.isLoading = false
                
                if let error = error {
                    self.showErrorMessage(error.localizedDescription)
                    return
                }
                
                self.errorMessage = "Инструкция по сбросу пароля отправлена на \(email)"
                self.showError = true
            }
        }
    }
    
    // MARK: - Firestore
    private func createUserDocument(uid: String, email: String, name: String) {
        let userData: [String: Any] = [
            "name": name,
            "email": email,
            "currency": "RUB",
            "monthlyIncome": 0,
            "createdAt": Timestamp(date: Date())
        ]
        
        db.collection("users").document(uid).setData(userData) { error in
            if let error = error {
                print("Error creating user document: \(error.localizedDescription)")
            } else {
                print("User document created successfully")
            }
        }
    }
    
    // MARK: - Error Handling
    private func showErrorMessage(_ message: String) {
        DispatchQueue.main.async {
            self.errorMessage = message
            self.showError = true
        }
    }
}
