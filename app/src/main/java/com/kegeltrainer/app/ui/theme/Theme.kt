package com.kegeltrainer.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val Scheme = darkColorScheme(
    primary = Teal,
    onPrimary = Night,
    secondary = Amber,
    onSecondary = Night,
    background = Night,
    onBackground = Ink,
    surface = NightElevated,
    onSurface = Ink,
    surfaceVariant = NightCard,
    onSurfaceVariant = InkMuted,
    outline = Line,
    error = Rose,
)

@Composable
fun KegelTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Scheme,
        typography = Typography,
        content = content,
    )
}
