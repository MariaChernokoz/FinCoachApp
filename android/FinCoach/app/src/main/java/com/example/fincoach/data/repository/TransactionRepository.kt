package com.example.fincoach.data.repository

import com.example.fincoach.data.model.Transaction
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class TransactionRepository {

    private val auth = Firebase.auth
    private val db   = Firebase.firestore

    private val uid get() = auth.currentUser?.uid ?: ""
    private fun col() = db.collection("transactions")

    fun observeTransactions(): Flow<List<Transaction>> = callbackFlow {
        val sub = col()
            .whereEqualTo("userId", uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    runCatching {
                        Transaction(
                            id            = doc.id,
                            userId        = doc.getString("userId") ?: "",
                            title         = doc.getString("title") ?: "",
                            amount        = doc.getDouble("amount") ?: 0.0,
                            categoryId    = doc.getString("categoryId") ?: "",
                            categoryTitle = doc.getString("categoryTitle") ?: "",
                            isIncome      = doc.getBoolean("isIncome") ?: false,
                            timestamp     = doc.getLong("timestamp") ?: 0L
                        )
                    }.getOrNull()
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { sub.remove() }
    }

    suspend fun addTransaction(transaction: Transaction): String {
        val data = mapOf(
            "userId"        to uid,
            "title"         to transaction.title,
            "amount"        to transaction.amount,
            "categoryId"    to transaction.categoryId,
            "categoryTitle" to transaction.categoryTitle,
            "isIncome"      to transaction.isIncome,
            "timestamp"     to transaction.timestamp
        )
        // document() сразу даёт ссылку с локальным id, set() кладёт запись в кеш.
        // await() здесь нельзя — иначе в офлайне будет вечная загрузка
        val doc = col().document()
        doc.set(data)
        return doc.id
    }

    suspend fun updateTransaction(transaction: Transaction) {
        val data = mapOf(
            "title"         to transaction.title,
            "amount"        to transaction.amount,
            "categoryId"    to transaction.categoryId,
            "categoryTitle" to transaction.categoryTitle,
            "isIncome"      to transaction.isIncome,
            "timestamp"     to transaction.timestamp
        )
        col().document(transaction.id).update(data)
    }

    suspend fun deleteTransaction(id: String) {
        col().document(id).delete()
    }
}