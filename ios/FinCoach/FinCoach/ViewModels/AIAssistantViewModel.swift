//
//  AIAssistantViewModel.swift
//  FinCoach
//
//  Created by Chernokoz on 13.02.2026.
//

import Foundation
import SwiftUI
import FirebaseFunctions
import Combine

class AIAssistantViewModel: ObservableObject {
    @Published var messages: [ChatMessage] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var showError = false
    
    private lazy var functions = Functions.functions(region: "us-central1")

    init() {
        loadChatHistory()
        functions.useEmulator(withHost: "127.0.0.1", port: 5001)
    }
    
    func sendMessage(_ text: String) {
        guard !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            return
        }
        
        let userMessage = ChatMessage(text: text, isUser: true)
        messages.append(userMessage)
        
        isLoading = true
        callAnalyzeFinances(question: text)
    }
    
    func loadExampleQuestion(_ question: String) {
        sendMessage(question)
    }
    
    func clearChat() {
        messages.removeAll()
        
        // TODO: Удалить историю из Firestore
    }
    
    
    // MARK: Cloud Function
    private func callAnalyzeFinances(question: String) {
        let data: [String: Any] = ["question": question]
        
        functions.httpsCallable("analyzeFinances").call(data) { [weak self] result, error in
            guard let self = self else { return }
            
            DispatchQueue.main.async {
                self.isLoading = false
                
                if let error = error as NSError? {
                    self.handleError(error)
                    return
                }
                
                if let data = result?.data as? [String: Any],
                   let answer = data["answer"] as? String {
                    
                    let aiMessage = ChatMessage(text: answer, isUser: false)
                    self.messages.append(aiMessage)
                } else {
                    self.showErrorMessage("Не удалось получить ответ от AI")
                }
            }
        }
    }
    
    private func handleError(_ error: NSError) {
        var message = "Произошла ошибка"
        
        if error.domain == FunctionsErrorDomain {
            let code = FunctionsErrorCode(rawValue: error.code)
            
            switch code {
            case .unauthenticated:
                message = "Необходимо войти в аккаунт"
            case .invalidArgument:
                message = "Некорректный вопрос"
            case .internal:
                message = "Внутренняя ошибка сервера"
            default:
                message = error.localizedDescription
            }
        }
        
        showErrorMessage(message)
        
        let errorMsg = ChatMessage(
            text: "Извините, произошла ошибка. Попробуйте еще раз.",
            isUser: false
        )
        messages.append(errorMsg)
    }
    
    private func showErrorMessage(_ message: String) {
        errorMessage = message
        showError = true
    }
    
    private func loadChatHistory() {
        
        // TODO: Загрузить историю из Firestore
        
        if messages.isEmpty {
            let welcomeMessage = ChatMessage(
                text: "Привет! Я ваш финансовый AI коуч. Задавайте мне вопросы о ваших тратах, целях и бюджете!",
                isUser: false
            )
            messages.append(welcomeMessage)
        }
    }
}
