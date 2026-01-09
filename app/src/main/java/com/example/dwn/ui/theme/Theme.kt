package com.example.dwn.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryPink,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3D1F2F),
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = PrimaryPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2D1F3D),
    onSecondaryContainer = Color(0xFFF3DAFF),
    tertiary = PrimaryBlue,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF1F2D3D),
    onTertiaryContainer = Color(0xFFD1E4FF),
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFF3D1F1F),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF4A4A4A),
    outlineVariant = Color(0xFF3A3A3A)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryPink,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E2),
    onPrimaryContainer = Color(0xFF3D1F2F),
    secondary = PrimaryPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3DAFF),
    onSecondaryContainer = Color(0xFF2D1F3D),
    tertiary = PrimaryBlue,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1E4FF),
    onTertiaryContainer = Color(0xFF1F2D3D),
    background = LightBackground,
    onBackground = Color(0xFF1A1A1A),
    surface = LightSurface,
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF505050),
    error = ErrorRed,
    onError = Color.White,
    outline = Color(0xFFD0D0D0),
    outlineVariant = Color(0xFFE0E0E0)
)

@Composable
fun DwnTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
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

