package com.example.fincoach.data.model

data class Goal(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val targetAmount: Double = 0.0,
    val savedAmount: Double = 0.0,
    val deadline: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false
) {
    val progress: Double
        get() = if (targetAmount > 0) (savedAmount / targetAmount).coerceIn(0.0, 1.0) else 0.0

    val remaining: Double
        get() = (targetAmount - savedAmount).coerceAtLeast(0.0)

    val isOverTarget: Boolean
        get() = savedAmount >= targetAmount
}