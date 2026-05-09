//
//  CategoriesService.swift
//  FinCoach
//
//  Created by Chernokoz on 03.05.2026.
//

import Foundation
import FirebaseFirestore

final class CategoriesService {
    private let db = Firestore.firestore()
    private let collectionName = "categories"
    
    func fetchCategories() async throws -> [Category] {
        let snapshot = try await db.collection(collectionName)
            .order(by: "title")
            .getDocuments()
        
        let categories = snapshot.documents.compactMap { document in
            Category(id: document.documentID, data: document.data())
        }
        
        return categories.isEmpty ? Category.fallbackCategories : categories
    }
}
