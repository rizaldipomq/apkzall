package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ZallDarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = Color(0xFF001F29),
    primaryContainer = Color(0xFF00384A),
    onPrimaryContainer = Color(0xFFB5F2FF),
    secondary = NeonCyan,
    onSecondary = Color(0xFF001F29),
    secondaryContainer = Color(0xFF0C3854),
    onSecondaryContainer = Color(0xFFC3E8FF),
    tertiary = ElectricIndigo,
    onTertiary = Color(0xFF13153B),
    background = ZallBackground,
    onBackground = TextPrimary,
    surface = ZallSurface,
    onSurface = TextPrimary,
    surfaceVariant = ZallSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = ZallCardBorder,
    error = DangerRed,
    onError = Color.White
)

@Composable
fun ZallBrowserTheme(
    darkTheme: Boolean = true, // Always premium dark mode for gaming focus
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ZallDarkColorScheme,
        typography = Typography,
        content = content
    )
}
