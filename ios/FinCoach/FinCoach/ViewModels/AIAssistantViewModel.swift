//
//  AIAssistantViewModel.swift
//  FinCoach
//

import Foundation
import SwiftUI
import FirebaseAuth
import FirebaseFirestore
import FirebaseFunctions
import Combine

final class AIAssistantViewModel: ObservableObject {
    @Published var messages: [ChatMessage] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var showError = false

    private lazy var functions = Functions.functions(region: "us-central1")
    private let db = Firestore.firestore()

    private var userId: String?
    private var authHandle: AuthStateDidChangeListenerHandle?

    private var messagesRef: CollectionReference? {
        guard let uid = userId else { return nil }
        return db.collection("users").document(uid).collection("chat_messages")
    }

    init() {
        // Fires immediately with current auth state AND whenever it changes.
        // This is more reliable than onAppear because Firebase restores auth
        // asynchronously from Keychain after force-quit.
        authHandle = Auth.auth().addStateDidChangeListener { [weak self] _, user in
            guard let self, let uid = user?.uid else { return }
            DispatchQueue.main.async { self.start(userId: uid) }
        }
    }

    deinit {
        if let handle = authHandle {
            Auth.auth().removeStateDidChangeListener(handle)
        }
    }

    func start(userId: String) {
        guard self.userId != userId else { return }
        self.userId = userId
        loadChatHistory()
    }

    // MARK: - Public

    func sendMessage(_ text: String) {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }

        let userMessage = ChatMessage(text: trimmed, isUser: true)
        messages.append(userMessage)
        save(userMessage)

        isLoading = true
        callAnalyzeFinances(with: ["message": trimmed])
    }

    func loadExampleQuestion(_ question: String) {
        sendMessage(question)
    }

    func clearChat() {
        messages.removeAll()
        messagesRef?.getDocuments { snapshot, _ in
            snapshot?.documents.forEach { $0.reference.delete() }
        }
    }

    // MARK: - Firestore load

    private func loadChatHistory() {
        guard let ref = messagesRef else {
            addWelcomeIfEmpty()
            return
        }

        ref.order(by: "timestamp", descending: false)
            .limit(to: 100)
            .getDocuments { [weak self] snapshot, error in
                guard let self else { return }
                DispatchQueue.main.async {
                    if let error {
                        print("Chat load error: \(error.localizedDescription)")
                        self.addWelcomeIfEmpty()
                        return
                    }
                    let loaded = snapshot?.documents.compactMap { doc in
                        self.messageFrom(data: doc.data(), id: doc.documentID)
                    } ?? []

                    self.messages = loaded.isEmpty ? [] : loaded
                    self.addWelcomeIfEmpty()
                }
            }
    }

    // MARK: - Firestore save

    private func save(_ message: ChatMessage) {
        guard let ref = messagesRef else { return }
        ref.document(message.id).setData(messageToDict(message)) { error in
            if let error { print("Chat save error: \(error.localizedDescription)") }
        }
    }

    // MARK: - Mapping helpers

    private func messageToDict(_ m: ChatMessage) -> [String: Any] {
        [
            "text": m.text,
            "isUser": m.isUser,
            "timestamp": Timestamp(date: m.timestamp)
        ]
    }

    private func messageFrom(data: [String: Any], id: String) -> ChatMessage? {
        guard
            let text = data["text"] as? String,
            let isUser = data["isUser"] as? Bool
        else { return nil }
        let timestamp = (data["timestamp"] as? Timestamp)?.dateValue() ?? Date()
        return ChatMessage(id: id, text: text, isUser: isUser, timestamp: timestamp)
    }

    private func addWelcomeIfEmpty() {
        guard messages.isEmpty else { return }
        messages.append(ChatMessage(
            text: "Привет! Я ваш финансовый AI коуч. Задавайте мне вопросы о ваших тратах, целях и бюджете!",
            isUser: false
        ))
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
                    self.save(aiMessage)
                } else {
                    self.showErrorMessage("Не удалось получить ответ от AI")
                }
            }
        }
    }

    private func handleError(_ error: NSError) {
        var message = "Произошла ошибка"
        if error.domain == FunctionsErrorDomain {
            switch FunctionsErrorCode(rawValue: error.code) {
            case .unauthenticated:  message = "Необходимо войти в аккаунт"
            case .invalidArgument:  message = "Сообщение слишком длинное или пустое"
            case .resourceExhausted: message = "Подождите несколько секунд перед следующим вопросом"
            case .internal:         message = "Внутренняя ошибка сервера"
            default:                message = error.localizedDescription
            }
        }
        showErrorMessage(message)
        messages.append(ChatMessage(text: "Извините, произошла ошибка. Попробуйте еще раз.", isUser: false))
    }

    private func showErrorMessage(_ message: String) {
        errorMessage = message
        showError = true
    }
}
