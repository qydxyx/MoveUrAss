package com.kegeltrainer.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** OLED dark: true black canvas so idle pixels stay off. Surfaces lift only a hair. */
internal val OledDarkScheme = darkColorScheme(
    primary = Color(0xFF4AD4C8),
    onPrimary = Color(0xFF003733),
    primaryContainer = Color(0xFF145A54),
    onPrimaryContainer = Color(0xFFB8F4EE),
    secondary = Color(0xFFF0B15A),
    onSecondary = Color(0xFF3D2800),
    secondaryContainer = Color(0xFF4A3410),
    onSecondaryContainer = Color(0xFFFFE0B0),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF3F3F4),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFF3F3F4),
    surfaceVariant = Color(0xFF161616),
    onSurfaceVariant = Color(0xFFA0A0A6),
    surfaceContainer = Color(0xFF0C0C0C),
    surfaceContainerHigh = Color(0xFF161616),
    surfaceContainerHighest = Color(0xFF1E1E1E),
    outline = Color(0xFF2C2C2C),
    outlineVariant = Color(0xFF222222),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF400010),
)

internal val LightScheme = lightColorScheme(
    primary = Color(0xFF0C7F77),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD4F4F0),
    onPrimaryContainer = Color(0xFF003733),
    secondary = Color(0xFFB56A12),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFE4C2),
    onSecondaryContainer = Color(0xFF3D2800),
    background = Color(0xFFF4F5F7),
    onBackground = Color(0xFF121417),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF121417),
    surfaceVariant = Color(0xFFECEEF1),
    onSurfaceVariant = Color(0xFF5E6570),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFECEEF1),
    surfaceContainerHighest = Color(0xFFE3E6EA),
    outline = Color(0xFFD5D8DE),
    outlineVariant = Color(0xFFE6E8EC),
    error = Color(0xFFC62828),
    onError = Color(0xFFFFFFFF),
)
