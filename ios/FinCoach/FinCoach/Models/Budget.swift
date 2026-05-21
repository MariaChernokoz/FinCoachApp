//
//  Budget.swift
//  FinCoach
//

import Foundation

struct Budget: Identifiable, Equatable {
    var id: String
    var userId: String
    var categoryId: String
    var limitAmount: Double
    var currentSpent: Double
    var period: String
    var periodStart: Int64
    var updatedAt: Int64

    var dictionary: [String: Any] {
        [
            "userId": userId,
            "categoryId": categoryId,
            "limitAmount": limitAmount,
            "currentSpent": currentSpent,
            "period": period,
            "periodStart": periodStart,
            "updatedAt": updatedAt
        ]
    }

    init(
        id: String,
        userId: String,
        categoryId: String,
        limitAmount: Double,
        currentSpent: Double = 0,
        period: String = "month",
        periodStart: Int64,
        updatedAt: Int64
    ) {
        self.id = id
        self.userId = userId
        self.categoryId = categoryId
        self.limitAmount = limitAmount
        self.currentSpent = currentSpent
        self.period = period
        self.periodStart = periodStart
        self.updatedAt = updatedAt
    }

    init?(id: String, data: [String: Any]) {
        guard
            let userId = data["userId"] as? String,
            let categoryId = data["categoryId"] as? String,
            let limitAmount = data["limitAmount"] as? Double
        else { return nil }
        self.id = id
        self.userId = userId
        self.categoryId = categoryId
        self.limitAmount = limitAmount
        self.currentSpent = data["currentSpent"] as? Double ?? 0
        self.period = data["period"] as? String ?? "month"
        self.periodStart = data["periodStart"] as? Int64 ?? 0
        self.updatedAt = data["updatedAt"] as? Int64 ?? 0
    }
}
