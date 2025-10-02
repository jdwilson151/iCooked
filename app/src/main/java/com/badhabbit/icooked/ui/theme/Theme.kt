package com.badhabbit.icooked.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    surface = cream02,           onSurface = brown,
    background = cream02,        onBackground = brown,
    primary = greenDark,                onPrimary = Color.White,
    primaryContainer = greenDark,        onPrimaryContainer = greenMint,
    secondary = greenDark,          onSecondary = Color.Black,
    secondaryContainer = greenDark, onSecondaryContainer = Color.White,
    tertiary = greenMint,            onTertiary = Color.Black,
    tertiaryContainer = greenMint,   onTertiaryContainer = Color.White
)

private val LightColorScheme = DarkColorScheme

@Composable
fun ICookedTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}