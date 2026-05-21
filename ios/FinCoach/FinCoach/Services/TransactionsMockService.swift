//
//  TransactionsMockService.swift
//  FinCoach
//
//  Created by Chernokoz on 16.02.2026.
//

import Foundation

final class TransactionsMockService {
    static let shared = TransactionsMockService()
    
    func getMockTransactions() -> [Transaction] {
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        
        return [
            Transaction(id: "1", title: "Starbucks", amount: 450, category: "Еда", isIncome: false, timestamp: now, userId: "mock"),
            Transaction(id: "2", title: "Такси", amount: 1200, category: "Транспорт", isIncome: false, timestamp: now, userId: "mock"),
            Transaction(id: "3", title: "Продукты", amount: 3500, category: "Еда", isIncome: false, timestamp: now, userId: "mock"),
            Transaction(id: "4", title: "Зарплата", amount: 80000, category: "Доход", isIncome: true, timestamp: now, userId: "mock")
        ]
    }
}
