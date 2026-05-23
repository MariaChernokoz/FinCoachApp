//
//  ChatMessageModel.swift
//  FinCoach
//
//  Created by Chernokoz on 13.02.2026.
//

import Foundation

struct ChatMessage: Identifiable, Equatable {
    let id: String
    let text: String
    let isUser: Bool
    let timestamp: Date

    init(id: String = UUID().uuidString, text: String, isUser: Bool, timestamp: Date = Date()) {
        self.id = id
        self.text = text
        self.isUser = isUser
        self.timestamp = timestamp
    }

    static func == (lhs: ChatMessage, rhs: ChatMessage) -> Bool {
        lhs.id == rhs.id
    }
}
