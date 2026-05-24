package com.example.fincoach.data.model

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val currency: String = "RUB",
    val totalBalance: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)
