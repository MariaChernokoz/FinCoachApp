//
//  TransactionsService.swift
//  FinCoach
//
//  Created by Chernokoz on 03.05.2026.
//

import Foundation
import FirebaseFirestore

final class TransactionsService {
    private let db = Firestore.firestore()
    private let collectionName = "transactions"
    
    func fetchTransactions(userId: String) async throws -> [Transaction] {
        let snapshot = try await db.collection(collectionName)
            .whereField("userId", isEqualTo: userId)
            .getDocuments()

        return Array(
            snapshot.documents
                .compactMap { Transaction(id: $0.documentID, data: $0.data()) }
                .sorted { $0.timestamp > $1.timestamp }
                .prefix(200)
        )
    }
    
    func create(_ transaction: Transaction) async throws {
        try await db.collection(collectionName)
            .document(transaction.id)
            .setData(transaction.dictionary)
    }
    
    func update(_ transaction: Transaction) async throws {
        try await db.collection(collectionName)
            .document(transaction.id)
            .setData(transaction.dictionary, merge: true)
    }
    
    func delete(_ transaction: Transaction) async throws {
        try await db.collection(collectionName)
            .document(transaction.id)
            .delete()
    }
}
