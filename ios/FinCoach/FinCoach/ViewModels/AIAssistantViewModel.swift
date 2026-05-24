//
//  AIAssistantViewModel.swift
//  FinCoach
//

import Foundation
import SwiftUI
import FirebaseAuth
import FirebaseFirestore
import FirebaseFunctions
import FirebaseFirestore
import FirebaseAuth
import Combine

final class AIAssistantViewModel: ObservableObject {
    @Published var messages: [ChatMessage] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var showError = false

    private lazy var functions = Functions.functions(region: "us-central1")
    private let db = Firestore.firestore()
    private var authListener: AuthStateDidChangeListenerHandle?
    private var userId: String?

    init() {
        authListener = Auth.auth().addStateDidChangeListener { [weak self] _, user in
            guard let self else { return }
            if let user {
                self.userId = user.uid
                self.loadMessages(userId: user.uid)
            } else {
                self.userId = nil
                self.messages = []
                self.addWelcomeMessage()
            }
        }
    }

    deinit {
        if let authListener {
            Auth.auth().removeStateDidChangeListener(authListener)
        }
    }

    func sendMessage(_ text: String) {
        guard !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }

        let userMessage = ChatMessage(text: text, isUser: true)
        messages.append(userMessage)
        saveMessage(userMessage)

        let transactions = TransactionsMockService.shared.getMockTransactions().map { $0.dictionary }
        let data: [String: Any] = ["question": text, "transactions": transactions]

        isLoading = true
        callAnalyzeFinances(with: ["message": trimmed])
    }

    func loadExampleQuestion(_ question: String) {
        sendMessage(question)
    }

    func clearChat() {
        messages.removeAll()
    }

    // MARK: - Firestore

    private func loadMessages(userId: String) {
        db.collection("users").document(userId).collection("chat_messages")
            .order(by: "timestamp", descending: false)
            .limit(to: 100)
            .getDocuments { [weak self] snapshot, _ in
                guard let self else { return }
                DispatchQueue.main.async {
                    if let docs = snapshot?.documents, !docs.isEmpty {
                        self.messages = docs.compactMap { doc in
                            let data = doc.data()
                            guard
                                let text = data["text"] as? String,
                                let isUser = data["isUser"] as? Bool,
                                let ts = data["timestamp"] as? Timestamp
                            else { return nil }
                            return ChatMessage(id: doc.documentID, text: text, isUser: isUser, timestamp: ts.dateValue())
                        }
                    } else {
                        self.addWelcomeMessage()
                    }
                }
            }
    }

    private func saveMessage(_ message: ChatMessage) {
        guard let userId else { return }
        let data: [String: Any] = [
            "text": message.text,
            "isUser": message.isUser,
            "timestamp": Timestamp(date: message.timestamp)
        ]
        db.collection("users").document(userId).collection("chat_messages")
            .document(message.id)
            .setData(data)
    }

    private func addWelcomeMessage() {
        if messages.isEmpty {
            messages.append(ChatMessage(
                text: Bundle.L("ai.welcome"),
                isUser: false
            ))
        }
    }

    // MARK: - Cloud Function

    private func callAnalyzeFinances(with data: [String: Any]) {
        functions.httpsCallable("analyzeFinances").call(data) { [weak self] result, error in
            guard let self else { return }

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
                    self.saveMessage(aiMessage)
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
            case .resourceExhausted:
                message = "Подождите несколько секунд перед следующим вопросом"
            case .internal:
                message = "Внутренняя ошибка сервера"
            default:
                message = error.localizedDescription
            }
        }

        showErrorMessage(message)

        let errorMsg = ChatMessage(text: "Извините, произошла ошибка. Попробуйте еще раз.", isUser: false)
        messages.append(errorMsg)
        saveMessage(errorMsg)
    }

    private func showErrorMessage(_ message: String) {
        errorMessage = message
        showError = true
    }
}
