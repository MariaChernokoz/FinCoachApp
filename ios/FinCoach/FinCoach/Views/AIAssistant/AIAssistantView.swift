//
//  AIAssistantView.swift
//  FinCoach
//
//  Created by Chernokoz on 09.02.2026.
//

import SwiftUI
import FirebaseAuth

struct AIAssistantView: View {
    @StateObject private var viewModel = AIAssistantViewModel()
    @EnvironmentObject private var authViewModel: AuthViewModel
    @EnvironmentObject private var navigationState: AppNavigationState
    @State private var messageText = ""
    @FocusState private var isInputFocused: Bool

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                QuoteOfTheDayView()
                    .padding(.horizontal, 16)
                    .padding(.top, 8)
                    .padding(.bottom, 4)
                    .background(AppColors.backgroundGray)

                // MARK: - Chat Messages

                ScrollViewReader { proxy in
                    ScrollView {
                        LazyVStack(spacing: 16) {
                            ForEach(viewModel.messages) { message in
                                MessageBubble(message: message)
                                    .id(message.id)
                            }

                            if viewModel.isLoading {
                                TypingIndicator()
                            }
                        }
                        .padding()
                    }
                    .background(AppColors.backgroundColor)
                }

                if viewModel.messages.count == 1 && !viewModel.isLoading {
                    ExampleQuestionsView { question in
                        viewModel.loadExampleQuestion(question)
                    }
                    .padding(.horizontal)
                    .padding(.bottom, 8)
                    .background(AppColors.backgroundColor)
                }

                Divider()

                // Input bar
                HStack(spacing: 12) {
                    TextField("Задайте вопрос...", text: $messageText, axis: .vertical)
                        .textFieldStyle(.plain)
                        .font(.system(size: 15))
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .background(AppColors.whiteFrameColor)
                        .cornerRadius(16)
                        .overlay(
                            RoundedRectangle(cornerRadius: 16)
                                .stroke(AppColors.lightGrayFrameColor, lineWidth: 1)
                        )
                        .lineLimit(1...5)
                        .focused($isInputFocused)
                        .onSubmit { sendMessage() }

                    Button {
                        sendMessage()
                    } label: {
                        Image(systemName: "arrow.up.circle.fill")
                            .font(.system(size: 32))
                            .foregroundColor(
                                messageText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                                ? AppColors.grayTextColor : AppColors.lightGreenFrameColor
                            )
                    }
                    .disabled(messageText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || viewModel.isLoading)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .background(AppColors.backgroundColor)
            }
            .background(AppColors.backgroundColor)
            .navigationTitle("Финансовый ассистент")
            .navigationBarTitleDisplayMode(.inline)
            .onAppear {
                if let userId = authViewModel.currentUser?.uid {
                    viewModel.start(userId: userId)
                }
            }
            .onChange(of: navigationState.pendingAIMessage) { message in
                guard let message else { return }
                navigationState.pendingAIMessage = nil
                viewModel.sendMessage(message)
            }
            .alert("Ошибка", isPresented: $viewModel.showError) {
                Button("OK", role: .cancel) {}
            } message: {
                Text(viewModel.errorMessage ?? "Произошла неизвестная ошибка")
            }
        }
    }

    private func sendMessage() {
        let text = messageText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else { return }
        viewModel.sendMessage(text)
        messageText = ""
        isInputFocused = false
    }
}

// MARK: - Message Bubble
struct MessageBubble: View {
    let message: ChatMessage

    var body: some View {
        HStack {
            if message.isUser { Spacer(minLength: 60) }

            VStack(alignment: message.isUser ? .trailing : .leading, spacing: 4) {
                Text(message.text)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(message.isUser ? AppColors.lightGreenFrameColor : AppColors.whiteFrameColor)
                    .foregroundColor(message.isUser ? .white : .primary)
                    .cornerRadius(16)
                    .shadow(
                        color: message.isUser ? .clear : Color.black.opacity(0.07),
                        radius: 4, x: 0, y: 2
                    )

                Text(formatTime(message.timestamp))
                    .font(.caption2)
                    .foregroundColor(AppColors.grayTextColor)
                    .padding(.horizontal, 4)
            }

            if !message.isUser { Spacer(minLength: 60) }
        }
    }

    private func formatTime(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.timeStyle = .short
        return formatter.string(from: date)
    }
}

// MARK: - Typing Indicator
struct TypingIndicator: View {
    @State private var animating = false

    var body: some View {
        HStack(spacing: 4) {
            ForEach(0..<3) { index in
                Circle()
                    .fill(AppColors.grayTextColor.opacity(0.5))
                    .frame(width: 8, height: 8)
                    .scaleEffect(animating ? 1.0 : 0.5)
                    .animation(
                        Animation.easeInOut(duration: 0.6)
                            .repeatForever()
                            .delay(Double(index) * 0.2),
                        value: animating
                    )
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(AppColors.whiteFrameColor)
        .cornerRadius(16)
        .shadow(color: Color.black.opacity(0.07), radius: 4, x: 0, y: 2)
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal)
        .id("typing")
        .onAppear { animating = true }
    }
}

// MARK: - Example Questions
struct ExampleQuestionsView: View {
    let onQuestionTap: (String) -> Void

    let exampleQuestions = [
        "Дай общую статистику моих трат",
        "Почему я трачу так много на еду?",
        "Успею ли я накопить на цель?",
        "Как оптимизировать мой бюджет?"
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Примеры вопросов:")
                .font(.system(size: 13, weight: .medium))
                .foregroundColor(AppColors.grayTextColor)
                .padding(.bottom, 2)

            ForEach(exampleQuestions, id: \.self) { question in
                Button {
                    onQuestionTap(question)
                } label: {
                    HStack(spacing: 12) {
//                        Image(systemName: "lightbulb.fill")
//                            .foregroundColor(AppColors.lightGreenFrameColor.opacity(0.8))
//                            .font(.system(size: 14))

                        Text(LocalizedStringKey(question))
                            .font(.system(size: 14))
                            .foregroundColor(AppColors.blackTextColor)
                            .multilineTextAlignment(.leading)

                        Spacer()

                        Image(systemName: "chevron.right")
                            .font(.system(size: 12))
                            .foregroundColor(AppColors.grayTextColor)
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 12)
                    .background(AppColors.whiteFrameColor)
                    .cornerRadius(16)
                    .shadow(color: Color.black.opacity(0.05), radius: 3, x: 0, y: 1)
                }
            }
        }
    }
}

#Preview {
    AIAssistantView()
        .environmentObject(AuthViewModel())
        .environmentObject(AppNavigationState())
}
