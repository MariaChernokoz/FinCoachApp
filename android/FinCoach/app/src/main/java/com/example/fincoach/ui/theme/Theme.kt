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
    primary              = BrightGreen,
    secondary            = DeepGreen,
    tertiary             = PurpleAccent,
    background           = NearBlack,
    surface              = DarkSurface,
    onBackground         = White,
    onSurface            = White,
    onSurfaceVariant     = DarkTextGray
)

private val LightColorScheme = lightColorScheme(
    primary              = BrightGreen,
    secondary            = DeepGreen,
    tertiary             = PurpleAccent,
    background           = BgLightGray,
    surface              = White,
    onBackground         = TextBlack,
    onSurface            = TextBlack,
    onSurfaceVariant     = TextGray
)

@Composable
fun FinCoachTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        window.statusBarColor = colorScheme.background.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}