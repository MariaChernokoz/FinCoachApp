//
//  Transaction.swift
//  FinCoach
//
//  Created by Chernokoz on 16.02.2026.
//

import Foundation

struct Transaction: Identifiable, Codable, Equatable {
    var id: String
    var title: String
    var amount: Double
    var category: String
    var isIncome: Bool
    var timestamp: Int64
    var userId: String
    
    var date: Date {
        Date(timeIntervalSince1970: TimeInterval(timestamp) / 1000)
    }
    
    var dictionary: [String: Any] {
        return [
            "title": title,
            "amount": amount,
            "category": category,
            "isIncome": isIncome,
            "timestamp": timestamp,
            "userId": userId
        ]
    }
    
    init(
        id: String,
        title: String,
        amount: Double,
        category: String,
        isIncome: Bool,
        timestamp: Int64,
        userId: String
    ) {
        self.id = id
        self.title = title
        self.amount = amount
        self.category = category
        self.isIncome = isIncome
        self.timestamp = timestamp
        self.userId = userId
    }
    
    init?(id: String, data: [String: Any]) {
        guard
            let title = data["title"] as? String,
            let isIncome = data["isIncome"] as? Bool,
            let userId = data["userId"] as? String,
            let amount = Transaction.doubleValue(from: data["amount"]),
            let timestamp = Transaction.int64Value(from: data["timestamp"])
        else {
            return nil
        }
        
        self.id = id
        self.title = title
        self.amount = amount
        self.category = data["category"] as? String ?? ""
        self.isIncome = isIncome
        self.timestamp = timestamp
        self.userId = userId
    }
    
    private static func doubleValue(from value: Any?) -> Double? {
        if let value = value as? Double {
            return value
        }
        if let value = value as? Int {
            return Double(value)
        }
        if let value = value as? NSNumber {
            return value.doubleValue
        }
        return nil
    }
    
    private static func int64Value(from value: Any?) -> Int64? {
        if let value = value as? Int64 {
            return value
        }
        if let value = value as? Int {
            return Int64(value)
        }
        if let value = value as? NSNumber {
            return value.int64Value
        }
        return nil
    }
}
