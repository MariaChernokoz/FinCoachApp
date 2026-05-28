package com.example.fincoach.data.repository

import com.example.fincoach.data.model.Goal
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class GoalRepository {

    private val auth = Firebase.auth
    private val db   = Firebase.firestore

    private val uid get() = auth.currentUser?.uid ?: ""
    private fun col() = db.collection("goals")

    fun observeGoals(): Flow<List<Goal>> = callbackFlow {
        val sub = col()
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    runCatching {
                        Goal(
                            id          = doc.id,
                            userId      = doc.getString("userId") ?: "",
                            title       = doc.getString("title") ?: "",
                            targetAmount = doc.getDouble("targetAmount") ?: 0.0,
                            savedAmount  = doc.getDouble("savedAmount") ?: 0.0,
                            deadline    = doc.getLong("deadline"),
                            createdAt   = doc.getLong("createdAt") ?: 0L,
                            isCompleted = doc.getBoolean("isCompleted") ?: false
                        )
                    }.getOrNull()
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                trySend(list)
            }
        awaitClose { sub.remove() }
    }

    suspend fun addGoal(goal: Goal) {
        val data = mapOf(
            "userId"       to uid,
            "title"        to goal.title,
            "targetAmount" to goal.targetAmount,
            "savedAmount"  to 0.0,
            "deadline"     to goal.deadline,
            "createdAt"    to System.currentTimeMillis(),
            "isCompleted"  to false
        )
        col().add(data).await()
    }

    suspend fun addSaving(goalId: String, amount: Double, currentSaved: Double, target: Double) {
        val newSaved = currentSaved + amount
        val isCompleted = newSaved >= target
        col().document(goalId).update(
            mapOf(
                "savedAmount"  to newSaved,
                "isCompleted"  to isCompleted
            )
        ).await()
    }

    suspend fun withdrawSaving(goalId: String, amount: Double, currentSaved: Double, target: Double) {
        val newSaved = maxOf(0.0, currentSaved - amount)
        val isCompleted = newSaved >= target
        col().document(goalId).update(
            mapOf(
                "savedAmount"  to newSaved,
                "isCompleted"  to isCompleted
            )
        ).await()
    }

    suspend fun deleteGoal(goalId: String) {
        col().document(goalId).delete().await()
    }
}