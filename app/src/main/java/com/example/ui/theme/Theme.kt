package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PrimeVideoColorScheme = darkColorScheme(
    primary = PrimeBlue,
    onPrimary = Color.Black,
    secondary = PrimeAccentBlue,
    onSecondary = Color.White,
    tertiary = PrimeCardBlue,
    background = PrimeDeepBlue,
    onBackground = Color.White,
    surface = PrimeCardBlue,
    onSurface = Color.White,
    surfaceVariant = PrimeCardBlue,
    onSurfaceVariant = Color.LightGray
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    // Streaming apps are dark-by-default to prevent eye strain and preserve cinematic contrast
    MaterialTheme(
        colorScheme = PrimeVideoColorScheme,
        typography = Typography,
        content = content
    )
}
