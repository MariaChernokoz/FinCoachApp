//
//  BudgetService.swift
//  FinCoach
//

import Foundation
import FirebaseFirestore

final class BudgetService {
    private let db = Firestore.firestore()
    private let collectionName = "budgets"

    func fetchBudgets(userId: String) async throws -> [Budget] {
        let snapshot = try await db.collection(collectionName)
            .whereField("userId", isEqualTo: userId)
            .getDocuments()
        return snapshot.documents.compactMap { Budget(id: $0.documentID, data: $0.data()) }
    }

    func create(_ budget: Budget) async throws {
        try await db.collection(collectionName)
            .document(budget.id)
            .setData(budget.dictionary)
    }

    func update(_ budget: Budget) async throws {
        try await db.collection(collectionName)
            .document(budget.id)
            .setData(budget.dictionary, merge: true)
    }

    func delete(_ budget: Budget) async throws {
        try await db.collection(collectionName)
            .document(budget.id)
            .delete()
    }
}
