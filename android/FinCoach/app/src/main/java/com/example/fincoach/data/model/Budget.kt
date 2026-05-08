package com.example.fincoach.data.model

import java.util.Calendar

data class Budget(
    val id: String = "",
    val userId: String = "",
    val categoryId: String = "",
    val limitAmount: Double = 0.0,
    val currentSpent: Double = 0.0,
    val period: String = "month",           // "month" или "week"
    val periodStart: Long = currentPeriodStart("month"),  // + новое: начало текущего периода
    val updatedAt: Long = System.currentTimeMillis()      // + новое: когда последний раз менялся
) {
    val spentFraction: Double
        get() = if (limitAmount > 0) (currentSpent / limitAmount).coerceIn(0.0, 1.0) else 0.0

    val remaining: Double
        get() = (limitAmount - currentSpent).coerceAtLeast(0.0)

    val isOverLimit: Boolean
        get() = currentSpent > limitAmount

    // Нужно ли сбросить current_spent (период закончился)?
    fun isPeriodExpired(): Boolean {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = periodStart }
        return when (period) {
            "month" -> {
                cal.add(Calendar.MONTH, 1)
                now >= cal.timeInMillis
            }
            "week" -> {
                cal.add(Calendar.WEEK_OF_YEAR, 1)
                now >= cal.timeInMillis
            }
            else -> false
        }
    }

    companion object {
        // Возвращает начало текущего месяца или недели в миллисекундах
        fun currentPeriodStart(period: String): Long {
            val cal = Calendar.getInstance()
            return when (period) {
                "month" -> {
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    cal.timeInMillis
                }
                "week" -> {
                    cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    cal.timeInMillis
                }
                else -> System.currentTimeMillis()
            }
        }
    }
}