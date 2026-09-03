package com.example.homeworkout.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ColorWhite = androidx.compose.ui.graphics.Color.White

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkTextPrimary,
    primaryContainer = DarkPrimary,
    onPrimaryContainer = DarkTextPrimary,
    secondary = DarkSecondary,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSecondary.copy(alpha = 0.22f),
    onSecondaryContainer = DarkTextPrimary,
    tertiary = DarkTertiary,
    onTertiary = DarkTextPrimary,
    tertiaryContainer = DarkTertiary.copy(alpha = 0.30f),
    onTertiaryContainer = DarkTextPrimary,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkTextSecondary.copy(alpha = 0.55f),
    outlineVariant = DarkTextSecondary.copy(alpha = 0.35f),
    error = DarkSecondary,
    errorContainer = DarkSecondary.copy(alpha = 0.18f)
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = ColorWhite,
    primaryContainer = LightPrimary.copy(alpha = 0.12f),
    onPrimaryContainer = LightTextPrimary,
    secondary = LightSecondary,
    onSecondary = LightTextPrimary,
    secondaryContainer = LightSecondary.copy(alpha = 0.18f),
    onSecondaryContainer = LightTextPrimary,
    tertiary = LightTertiary,
    onTertiary = ColorWhite,
    tertiaryContainer = LightTertiary.copy(alpha = 0.14f),
    onTertiaryContainer = LightTextPrimary,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightTextSecondary.copy(alpha = 0.55f),
    outlineVariant = LightTextSecondary.copy(alpha = 0.25f),
    error = LightSecondary,
    errorContainer = LightSecondary.copy(alpha = 0.12f)
)

@Composable
fun HomeWorkoutTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
