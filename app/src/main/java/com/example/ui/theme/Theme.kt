package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SummerDarkColorScheme = darkColorScheme(
    primary = CyanLuminous,
    onPrimary = CoreBlack,
    primaryContainer = CyanMuted,
    onPrimaryContainer = CyanBright,
    secondary = SlateLight,
    onSecondary = CoreBlack,
    secondaryContainer = CoreCharcoalElevated,
    onSecondaryContainer = SlateBright,
    tertiary = CyanGlow,
    onTertiary = CoreBlack,
    background = CoreBlack,
    onBackground = CoreWhite,
    surface = CoreDarkCharcoal,
    onSurface = CoreWhite,
    surfaceVariant = CoreCharcoalSurface,
    onSurfaceVariant = SlateLight,
    outline = CoreCharcoalBorder,
    outlineVariant = CoreCharcoalElevated
)

@Composable
fun SummerWinterTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SummerDarkColorScheme,
        typography = Typography,
        content = content
    )
}
