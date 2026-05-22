package com.example.fincoach.data.model

data class ChatMessage(
    val id: String = "",
    val userId: String = "",
    val text: String = "",
    val isUser: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
