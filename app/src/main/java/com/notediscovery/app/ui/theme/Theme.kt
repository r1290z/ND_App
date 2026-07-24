package com.notediscovery.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkBackground = Color(0xFF0A0A1A)
private val DarkSurface = Color(0xFF111128)
private val DarkSurfaceVariant = Color(0xFF1A1A3E)
private val AccentBlue = Color(0xFF3D98FF)
private val AccentPurple = Color(0xFFA855F7)
private val OnBackground = Color(0xFFE0E0E0)
private val OnSurface = Color(0xFFCCCCDD)
private val TextSecondary = Color(0xFF667799)
private val CardBackground = Color(0xFF162040)

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    secondary = AccentPurple,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = OnBackground,
    onSurface = OnSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    outline = Color(0xFF2A2A4A),
    surfaceTint = CardBackground
)

@Composable
fun NoteDiscoveryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(),
        content = content
    )
}
