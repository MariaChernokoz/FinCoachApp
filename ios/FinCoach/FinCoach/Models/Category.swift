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
        let raw = (data["iconName"] as? String) ?? (data["icon"] as? String) ?? ""
        self.icon = Category.resolvedIcon(raw)
        self.isDefault = data["isDefault"] as? Bool ?? false
    }

    // Maps Material Design / legacy names → SF Symbols
    static func resolvedIcon(_ name: String) -> String {
        let map: [String: String] = [
            // Material → SF Symbol
            "cup":                    "cup.and.saucer",
            "movie":                  "film",
            "shopping_cart":          "cart",
            "restaurant":             "fork.knife",
            "directions_bus":         "bus",
            "child":                  "person.2",
            "paw":                    "pawprint",
            "favorite":               "heart",
            "checkroom":              "tshirt",
            "arrow_turn":             "arrow.uturn.left",
            "chart_line_uptrend":     "chart.line.uptrend.xyaxis",
            "category":               "ellipsis.circle",
            "work":                   "briefcase",
            "card_giftcard":          "gift",
            "laptop":                 "laptopcomputer",
            "computer":               "desktopcomputer",
            // common aliases
            "percent":                "percent",
            "house":                  "house",
            "house.fill":             "house.fill",
            "airplane":               "airplane",
            "bolt":                   "bolt",
            "sparkles":               "sparkles",
            "repeat":                 "repeat",
            "pawprint":               "pawprint",
            "wrench.and.screwdriver": "wrench.and.screwdriver",
            "graduationcap":          "graduationcap",
            "creditcard":             "creditcard",
            "figure.run":             "figure.run",
        ]
        let resolved = map[name] ?? name
        return resolved.isEmpty ? "tag" : resolved
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
