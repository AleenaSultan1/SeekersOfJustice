package com.seekersofjustice.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B5E20),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA5D6A7),
    onPrimaryContainer = Color(0xFF052E0B),
    secondary = Color(0xFF52634F),
    secondaryContainer = Color(0xFFD5E8CF),
    tertiary = Color(0xFF38656A),
    background = Color(0xFFFBFDF7),
    surface = Color(0xFFFBFDF7),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8BC98F),
    onPrimary = Color(0xFF0A390F),
    primaryContainer = Color(0xFF255428),
    onPrimaryContainer = Color(0xFFA5D6A7),
    secondary = Color(0xFFB9CCB4),
    secondaryContainer = Color(0xFF3A4B38),
    tertiary = Color(0xFFA0CFD4),
)

@Composable
fun SeekersTheme(content: @Composable () -> Unit) {
    val isUrdu = LocalConfiguration.current.locales[0]?.language == "ur"
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = seekersTypography(isUrdu),
        content = content,
    )
}
