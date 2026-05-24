package com.example.fincoach.data.model

data class Category(
    val id: String = "",
    val title: String = "",
    val type: String = "expense",   // "income" или "expense"
    val iconName: String = ""
)