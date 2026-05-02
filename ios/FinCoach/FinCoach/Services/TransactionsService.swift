//
//  TransactionsService.swift
//  FinCoach
//
//  Created by Chernokoz on 16.02.2026.
//

import Foundation

final class TransactionsService {
    static let shared = TransactionsService()
    
    func getMockTransactions() -> [Transaction] {
        return [
            Transaction(id: "1", title: "Starbucks", amount: 450, category: "Еда", date: Date()),
            Transaction(id: "2", title: "Такси", amount: 1200, category: "Транспорт", date: Date()),
            Transaction(id: "3", title: "Продукты", amount: 3500, category: "Еда", date: Date()),
            Transaction(id: "4", title: "Starbucks", amount: 550, category: "Кофе", date: Date()),
            Transaction(id: "5", title: "Такси", amount: 1350, category: "Транспорт", date: Date()),
            Transaction(id: "6", title: "Продукты", amount: 4500, category: "Еда", date: Date())
        ]
    }
}
