//
//  ChatMessageModel.swift
//  FinCoach
//
//  Created by Chernokoz on 13.02.2026.
//

import Foundation

struct ChatMessage: Identifiable, Equatable {
    let id = UUID()
    let text: String
    let isUser: Bool
    let timestamp = Date()
    
    static func == (lhs: ChatMessage, rhs: ChatMessage) -> Bool {
        lhs.id == rhs.id
    }
}
