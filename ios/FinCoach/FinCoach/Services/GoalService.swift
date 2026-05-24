//
//  GoalService.swift
//  FinCoach
//

import Foundation
import FirebaseFirestore

final class GoalService {
    private let db = Firestore.firestore()
    private let collectionName = "goals"

    func fetchGoals(userId: String) async throws -> [Goal] {
        let snapshot = try await db.collection(collectionName)
            .whereField("userId", isEqualTo: userId)
            .getDocuments()
        return snapshot.documents
            .compactMap { Goal(id: $0.documentID, data: $0.data()) }
            .sorted { $0.createdAt > $1.createdAt }
    }

    func create(_ goal: Goal) async throws {
        try await db.collection(collectionName)
            .document(goal.id)
            .setData(goal.dictionary)
    }

    func update(_ goal: Goal) async throws {
        try await db.collection(collectionName)
            .document(goal.id)
            .setData(goal.dictionary, merge: true)
    }

    func delete(_ goal: Goal) async throws {
        try await db.collection(collectionName)
            .document(goal.id)
            .delete()
    }
}
