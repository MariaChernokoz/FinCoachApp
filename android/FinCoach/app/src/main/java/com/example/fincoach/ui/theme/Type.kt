package com.example.fincoach.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    // Заголовок "Welcome to FinCoach AI!"
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = 0.5.sp,
        color = White // Белый на зеленом фоне
    ),
    // Подзаголовок "Your personal finance coach"
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        color = White.copy(alpha = 0.9f)
    ),
    // Основной текст (например, в полях ввода)
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        letterSpacing = 0.5.sp,
        color = TextBlack // Используем #000000 из палитры
    ),
    // Вспомогательный текст (плейсхолдеры, "Забыли пароль?")
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        color = TextGray // Используем #7C7C7C из палитры
    ),
    // Текст на кнопках
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        letterSpacing = 1.sp,
        color = White
    )
)