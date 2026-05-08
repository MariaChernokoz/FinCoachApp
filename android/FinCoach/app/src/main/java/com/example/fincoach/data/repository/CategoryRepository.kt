package com.example.fincoach.data.repository

import com.example.fincoach.data.model.Category
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CategoryRepository {

    // Инициализируем Firestore
    private val db = Firebase.firestore

    // Ссылка на общую коллекцию категорий.
    private fun col() = db.collection("categories")


    // Получаем данные НАПРЯМУЮ ИЗ БД в реальном времени
    fun observeCategories(): Flow<List<Category>> = callbackFlow {
        val subscription = col().addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val items = snapshot?.documents?.mapNotNull { doc ->
                runCatching {
                    Category(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        type = doc.getString("type") ?: "expense",
                        iconName = doc.getString("iconName") ?: ""
                    )
                }.getOrNull()
            } ?: emptyList()
            trySend(items)
        }
        awaitClose { subscription.remove() }
    }

    /**
     * Разовое получение всех категорий из базы данных.
     */
    suspend fun getCategories(): List<Category> {
        val snap = col().get().await()
        return snap.documents.mapNotNull { doc ->
            runCatching {
                Category(
                    id = doc.id,
                    title = doc.getString("title") ?: "",
                    type = doc.getString("type") ?: "expense",
                    iconName = doc.getString("iconName") ?: ""
                )
            }.getOrNull()
        }
    }
}
