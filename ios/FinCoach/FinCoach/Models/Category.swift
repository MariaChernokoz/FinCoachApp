//
//  Category.swift
//  FinCoach
//
//  Created by Chernokoz on 03.05.2026.
//

import Foundation

enum TransactionType: String, CaseIterable, Identifiable, Codable {
    case expense
    case income
    
    var id: String { rawValue }
    
    var title: String {
        switch self {
        case .expense:
            return "Expense"
        case .income:
            return "Income"
        }
    }
}

struct Category: Identifiable, Codable, Equatable {
    var id: String
    var title: String
    var type: TransactionType
    var icon: String
    var isDefault: Bool
    
    init(
        id: String,
        title: String,
        type: TransactionType,
        icon: String,
        isDefault: Bool
    ) {
        self.id = id
        self.title = title
        self.type = type
        self.icon = icon.isEmpty ? "tag" : icon
        self.isDefault = isDefault
    }
    
    init?(id: String, data: [String: Any]) {
        guard
            let title = data["title"] as? String,
            let typeValue = data["type"] as? String,
            let type = TransactionType(rawValue: typeValue)
        else {
            return nil
        }
        
        self.id = id
        self.title = title
        self.type = type
        let icon = data["icon"] as? String ?? "tag"
        self.icon = icon.isEmpty ? "tag" : icon
        self.isDefault = data["isDefault"] as? Bool ?? false
    }
    
    static let fallbackCategories: [Category] = [
        Category(id: "fallback-food", title: "Продукты", type: .expense, icon: "cart", isDefault: true),
        Category(id: "fallback-transport", title: "Транспорт", type: .expense, icon: "car", isDefault: true),
        Category(id: "fallback-coffee", title: "Кофе", type: .expense, icon: "cup.and.saucer", isDefault: true),
        Category(id: "fallback-shopping", title: "Покупки", type: .expense, icon: "bag", isDefault: true),
        Category(id: "fallback-health", title: "Здоровье", type: .expense, icon: "cross.case", isDefault: true),
        Category(id: "fallback-salary", title: "Зарплата", type: .income, icon: "wallet.pass", isDefault: true),
        Category(id: "fallback-bonus", title: "Бонус", type: .income, icon: "gift", isDefault: true)
    ]
}
