package com.example.fincoach.data.repository

import com.example.fincoach.data.model.Budget
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class BudgetRepository {

    private val auth = Firebase.auth
    private val db   = Firebase.firestore

    private val uid get() = auth.currentUser?.uid ?: ""
    private fun col() = db.collection("budgets")

    fun observeBudgets(): Flow<List<Budget>> = callbackFlow {
        val sub = col()
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    runCatching {
                        Budget(
                            id            = doc.id,
                            userId       = doc.getString("userId") ?: "",
                            categoryId   = doc.getString("categoryId") ?: "",
                            limitAmount  = doc.getDouble("limitAmount") ?: 0.0,
                            currentSpent = doc.getDouble("currentSpent") ?: 0.0,
                            period        = doc.getString("period") ?: "month",
                            periodStart  = doc.getLong("periodStart") ?: 0L,
                            updatedAt    = doc.getLong("updatedAt") ?: 0L
                        )
                    }.getOrNull()
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { sub.remove() }
    }

    suspend fun setBudget(budget: Budget) {
        val now = System.currentTimeMillis()
        val data = mapOf(
            "userId"       to uid,
            "categoryId"   to budget.categoryId,
            "limitAmount"  to budget.limitAmount,
            "currentSpent" to budget.currentSpent,
            "period"        to budget.period,
            "periodStart"  to Budget.currentPeriodStart(budget.period),
            "updatedAt"    to now
        )
        if (budget.id.isEmpty()) {
            col().add(data).await()
        } else {
            col().document(budget.id).set(data).await()
        }
    }

    suspend fun updateSpent(budgetId: String, newSpent: Double) {
        col().document(budgetId).update(
            mapOf(
                "currentSpent" to newSpent,
                "updatedAt"    to System.currentTimeMillis()
            )
        ).await()
    }

    // Сбрасываем current_spent если период закончился
    suspend fun resetExpiredBudgets(budgets: List<Budget>) {
        budgets.filter { it.isPeriodExpired() }.forEach { budget ->
            col().document(budget.id).update(
                mapOf(
                    "currentSpent" to 0.0,
                    "periodStart"  to Budget.currentPeriodStart(budget.period),
                    "updatedAt"    to System.currentTimeMillis()
                )
            ).await()
        }
    }

    suspend fun deleteBudget(id: String) {
        col().document(id).delete().await()
    }
}