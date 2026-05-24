//
//  Goal.swift
//  FinCoach
//

import Foundation

struct Goal: Identifiable, Equatable {
    var id: String
    var userId: String
    var title: String
    var targetAmount: Double
    var savedAmount: Double
    var deadline: Int64?
    var isCompleted: Bool
    var createdAt: Int64

    var progress: Double {
        guard targetAmount > 0 else { return 0 }
        return min(savedAmount / targetAmount, 1.0)
    }

    var dictionary: [String: Any] {
        var dict: [String: Any] = [
            "userId": userId,
            "title": title,
            "targetAmount": targetAmount,
            "savedAmount": savedAmount,
            "isCompleted": isCompleted,
            "createdAt": createdAt
        ]
        if let deadline {
            dict["deadline"] = deadline
        } else {
            dict["deadline"] = NSNull()
        }
        return dict
    }

    init(
        id: String,
        userId: String,
        title: String,
        targetAmount: Double,
        savedAmount: Double = 0,
        deadline: Int64? = nil,
        isCompleted: Bool = false,
        createdAt: Int64
    ) {
        self.id = id
        self.userId = userId
        self.title = title
        self.targetAmount = targetAmount
        self.savedAmount = savedAmount
        self.deadline = deadline
        self.isCompleted = isCompleted
        self.createdAt = createdAt
    }

    init?(id: String, data: [String: Any]) {
        guard
            let userId = data["userId"] as? String,
            let title = data["title"] as? String,
            let targetAmount = Goal.doubleValue(from: data["targetAmount"])
        else { return nil }
        self.id = id
        self.userId = userId
        self.title = title
        self.targetAmount = targetAmount
        self.savedAmount = Goal.doubleValue(from: data["savedAmount"]) ?? 0
        self.isCompleted = data["isCompleted"] as? Bool ?? false
        self.createdAt = Goal.int64Value(from: data["createdAt"]) ?? 0
        if let dl = Goal.int64Value(from: data["deadline"]), dl > 0 {
            self.deadline = dl
        } else {
            self.deadline = nil
        }
    }

    private static func doubleValue(from value: Any?) -> Double? {
        if let v = value as? Double { return v }
        if let v = value as? Int { return Double(v) }
        if let v = value as? NSNumber { return v.doubleValue }
        return nil
    }

    private static func int64Value(from value: Any?) -> Int64? {
        if let v = value as? Int64 { return v }
        if let v = value as? Int { return Int64(v) }
        if let v = value as? NSNumber { return v.int64Value }
        return nil
    }
}
