//
//  Transaction.swift
//  FinCoach
//
//  Created by Chernokoz on 16.02.2026.
//

import Foundation

struct Transaction: Codable {
    let id: String
    let title: String
    let amount: Double
    let category: String
    let date: Date
    
    var dictionary: [String: Any] {
        return [
            "id": id,
            "title": title,
            "amount": amount,
            "category": category,
            "date": date.timeIntervalSince1970
        ]
    }
}
