package com.seekersofjustice.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration

// Institutional identity: navy leads, official red is reserved for the crest
// and critical/destructive moments so the two strong colours never compete.
private val Navy = Color(0xFF12386B)
private val NavyDeep = Color(0xFF0C2A52)
private val NavyTint = Color(0xFFE8EFF9)
private val Crimson = Color(0xFFB21F2B)
private val CrimsonTint = Color(0xFFFBECEC)

private val LightColors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    primaryContainer = NavyTint,
    onPrimaryContainer = NavyDeep,
    secondary = Color(0xFF4A5A72),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE4F0),
    onSecondaryContainer = Color(0xFF1B2838),
    tertiary = Crimson,
    onTertiary = Color.White,
    tertiaryContainer = CrimsonTint,
    onTertiaryContainer = Color(0xFF5C0E14),
    error = Crimson,
    onError = Color.White,
    errorContainer = CrimsonTint,
    onErrorContainer = Color(0xFF5C0E14),
    background = Color(0xFFF6F8FC),
    onBackground = Color(0xFF0F1E33),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F1E33),
    surfaceVariant = Color(0xFFE7ECF4),
    onSurfaceVariant = Color(0xFF566579),
    outline = Color(0xFF97A3B6),
    outlineVariant = Color(0xFFD7DEEA),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9EC0F0),
    onPrimary = Color(0xFF10233F),
    primaryContainer = Color(0xFF1D3E6D),
    onPrimaryContainer = NavyTint,
    secondary = Color(0xFFB9C6DC),
    onSecondary = Color(0xFF243346),
    secondaryContainer = Color(0xFF33435C),
    onSecondaryContainer = Color(0xFFD9E2F1),
    tertiary = Color(0xFFF2A3A9),
    onTertiary = Color(0xFF5C0E14),
    tertiaryContainer = Color(0xFF7E2029),
    onTertiaryContainer = Color(0xFFFFDADC),
    error = Color(0xFFF2A3A9),
    onError = Color(0xFF5C0E14),
    errorContainer = Color(0xFF7E2029),
    onErrorContainer = Color(0xFFFFDADC),
    background = Color(0xFF0A1220),
    onBackground = Color(0xFFE6ECF5),
    surface = Color(0xFF12203A),
    onSurface = Color(0xFFE6ECF5),
    surfaceVariant = Color(0xFF1C2B48),
    onSurfaceVariant = Color(0xFF9DAAC0),
    outline = Color(0xFF5D6B84),
    outlineVariant = Color(0xFF22314E),
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
