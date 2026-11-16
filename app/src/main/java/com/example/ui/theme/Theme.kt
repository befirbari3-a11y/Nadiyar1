package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppLightColorScheme = lightColorScheme(
    primary = RoyalBlue,
    onPrimary = Color.White,
    primaryContainer = RoyalBlueContainer,
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = TealPrimary,
    onSecondary = Color.White,
    secondaryContainer = TealContainer,
    onSecondaryContainer = Color(0xFF115E59),
    tertiary = VioletMath,
    onTertiary = Color.White,
    tertiaryContainer = VioletContainer,
    onTertiaryContainer = Color(0xFF4C1D95),
    background = SurfaceBackground,
    onBackground = SlateNavy,
    surface = CardSurface,
    onSurface = SlateNavy,
    surfaceVariant = Color(0xFFF8FAFC),
    onSurfaceVariant = MutedText,
    outline = SubtleBorder,
    error = CrimsonDanger,
    onError = Color.White
)

@Composable
fun NadiyarExamTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppLightColorScheme,
        typography = Typography,
        content = content
    )
}
