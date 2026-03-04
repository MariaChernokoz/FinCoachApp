package com.example.fincoach.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = BrightGreen,
    secondary = DeepGreen,
    tertiary = PurpleAccent,
    background = DeepGreen,
    surface = White
)

private val LightColorScheme = lightColorScheme(
    primary = BrightGreen,
    secondary = DeepGreen,
    tertiary = PurpleAccent,
    background = DeepGreen,
    surface = White
)

@Composable
fun FinCoachTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Выключаем dynamicColor по умолчанию (ставим false)
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // Код для того, чтобы верхняя полоска (Status Bar) тоже была в цвет фона
    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        window.statusBarColor = DeepGreen.toArgb() // Ставим цвет #77D43C
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}