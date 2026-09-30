package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CyanLuminous,
    onPrimary = CoreBlack,
    primaryContainer = CoreCharcoalElevated,
    onPrimaryContainer = CyanBright,
    secondary = SlateLight,
    onSecondary = CoreBlack,
    background = CoreBlack,
    onBackground = SlateBright,
    surface = CoreCharcoalSurface,
    onSurface = SlateBright,
    surfaceVariant = CoreCharcoalElevated,
    onSurfaceVariant = SlateLight,
    outline = CoreCharcoalBorder,
    error = ErrorRed,
    onError = SlateBright
)

@Composable
fun SummerWinterTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
