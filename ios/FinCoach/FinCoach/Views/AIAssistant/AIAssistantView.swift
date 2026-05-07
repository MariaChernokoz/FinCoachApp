//
//  AIAssistantView.swift
//  FinCoach
//
//  Created by Chernokoz on 09.02.2026.
//

import SwiftUI

struct AIAssistantView: View {
    @StateObject private var viewModel = AIAssistantViewModel()
    @State private var messageText = ""
    @FocusState private var isInputFocused: Bool
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
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
//                    .onChange(of: viewModel.messages.count) { _ in
//                        if let lastMessage = viewModel.messages.last {
//                            withAnimation {
//                                proxy.scrollTo(lastMessage.id, anchor: .bottom)
//                            }
//                        }
//                    }
                }
                
                if viewModel.messages.count == 1 && !viewModel.isLoading {
                    ExampleQuestionsView { question in
                        viewModel.loadExampleQuestion(question)
                    }
                    .padding(.horizontal)
                    .padding(.bottom)
                }
                
                Divider()
                    .padding(.bottom, 8)
                
                HStack(spacing: 12) {
                    TextField("Задайте вопрос...", text: $messageText, axis: .vertical)
                        .textFieldStyle(.plain)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .background(Color.gray.opacity(0.1))
                        .cornerRadius(20)
                        .lineLimit(1...5)
                        .focused($isInputFocused)
                        .onSubmit {
                            sendMessage()
                        }
                    
                    Button {
                        sendMessage()
                    } label: {
                        Image(systemName: "arrow.up.circle.fill")
                            .font(.system(size: 32))
                            .foregroundColor(
                                messageText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                                ? .gray
                                : AppColors.lightGreenFrameColor
                            )
                    }
                    .disabled(messageText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || viewModel.isLoading)
                }
                .padding(.horizontal)
                .padding(.vertical, 8)
                .background(Color(.systemBackground))
            }
            .navigationTitle("AI Коуч")
            .navigationBarTitleDisplayMode(.inline)
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
            if message.isUser {
                Spacer(minLength: 60)
            }
            
            VStack(alignment: message.isUser ? .trailing : .leading, spacing: 4) {
                Text(message.text)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(
                        message.isUser
                        ? AppColors.lightGreenFrameColor
                        : Color.gray.opacity(0.15)
                    )
                    .foregroundColor(message.isUser ? .white : .primary)
                    .cornerRadius(18)
                
                Text(formatTime(message.timestamp))
                    .font(.caption2)
                    .foregroundColor(.gray)
                    .padding(.horizontal, 4)
            }
            
            if !message.isUser {
                Spacer(minLength: 60)
            }
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
                    .fill(Color.gray.opacity(0.6))
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
        .background(Color.gray.opacity(0.15))
        .cornerRadius(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal)
        .id("typing")
        .onAppear {
            animating = true
        }
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
        VStack(alignment: .leading, spacing: 12) {
            Text("Примеры вопросов:")
                .font(.subheadline)
                .foregroundColor(.gray)
            
            ForEach(exampleQuestions, id: \.self) { question in
                Button {
                    onQuestionTap(question)
                } label: {
                    HStack {
                        Image(systemName: "lightbulb.fill")
                            .foregroundColor(Color.yellow.opacity(0.7))
                            .font(.subheadline)
                        
                        Text(question)
                            .font(.subheadline)
                            .foregroundColor(.primary)
                            .multilineTextAlignment(.leading)
                        
                        Spacer()
                        
                        Image(systemName: "chevron.right")
                            .font(.caption)
                            .foregroundColor(.gray)
                    }
                    .padding()
                    .background(Color.gray.opacity(0.05))
                    .cornerRadius(12)
                }
            }
        }
    }
}

#Preview {
    AIAssistantView()
}
