package com.example.fincoach.data.model

data class Transaction(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val categoryId: String = "",
    val categoryTitle: String = "",
    val isIncome: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)