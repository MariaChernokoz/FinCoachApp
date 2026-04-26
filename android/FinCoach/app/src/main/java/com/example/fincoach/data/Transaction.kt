package com.example.fincoach.data

data class Transaction(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)