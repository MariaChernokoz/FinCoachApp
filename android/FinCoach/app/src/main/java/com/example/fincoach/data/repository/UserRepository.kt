package com.example.fincoach.data.repository

import com.example.fincoach.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val auth = Firebase.auth
    private val db   = Firebase.firestore

    private val uid get() = auth.currentUser?.uid ?: ""

    // Функция для отслеживания состояния авторизации
    fun currentUserFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            this.trySend(auth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    private fun userDoc() = db.collection("users").document(uid)

    suspend fun createUserProfile(name: String, email: String) {
        val user = mapOf(
            "name"         to name,
            "email"        to email,
            "currency"     to "RUB",
            "totalBalance" to 0.0, // camelCase
            "createdAt"    to System.currentTimeMillis()
        )
        userDoc().set(user).await()
    }

    suspend fun getUserProfile(): User? {
        if (uid.isEmpty()) return null // Защита от пустого UID
        val snap = userDoc().get().await()
        if (!snap.exists()) return null
        return User(
            id           = snap.id,
            name         = snap.getString("name") ?: "",
            email        = snap.getString("email") ?: "",
            currency     = snap.getString("currency") ?: "RUB",
            totalBalance = snap.getDouble("totalBalance") ?: 0.0, // camelCase
            createdAt    = snap.getLong("createdAt") ?: 0L
        )
    }

    suspend fun updateBalance(newBalance: Double) {
        // без await() — чтобы работало в офлайне, запись уйдёт в кеш и синхронизируется потом
        userDoc().update("totalBalance", newBalance)
    }

    suspend fun updateCurrency(currency: String) {
        userDoc().update("currency", currency)
    }
}