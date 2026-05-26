package com.example.fincoach.data.repository

import com.example.fincoach.data.model.ChatMessage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository {

    private val auth = Firebase.auth
    private val db   = Firebase.firestore

    private val uid get() = auth.currentUser?.uid ?: ""
    private fun col() = db.collection("chat_messages")

    fun observeMessages(): Flow<List<ChatMessage>> = callbackFlow {
        val sub = col()
            .whereEqualTo("userId", uid)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    runCatching {
                        ChatMessage(
                            id        = doc.id,
                            userId    = doc.getString("userId") ?: "",
                            text      = doc.getString("text") ?: "",
                            isUser    = doc.getBoolean("isUser") ?: false,
                            timestamp = doc.getLong("timestamp") ?: 0L
                        )
                    }.getOrNull()
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { sub.remove() }
    }

    suspend fun addMessage(text: String, isUser: Boolean): String {
        val data = mapOf(
            "userId"    to uid,
            "text"      to text,
            "isUser"    to isUser,
            "timestamp" to System.currentTimeMillis()
        )
        // document() даёт локальный id сразу, set() без await() — чтобы не зависало в офлайне
        val doc = col().document()
        doc.set(data)
        return doc.id
    }

    // Удаляем всю переписку пользователя
    suspend fun clearChat() {
        val docs = col().whereEqualTo("userId", uid).get().await()
        for (doc in docs.documents) {
            doc.reference.delete()   // без await() — иначе зависнет на первом же документе
        }
    }
}
