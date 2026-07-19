package com.seekersofjustice.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.seekersofjustice.app.R

private val NastaliqFamily = FontFamily(Font(R.font.noto_nastaliq_urdu))

/**
 * Urdu uses Noto Nastaliq, which has steep descenders: line heights must be
 * ~2x the font size or the script gets clipped at Material defaults.
 */
fun seekersTypography(isUrdu: Boolean): Typography {
    if (!isUrdu) return Typography()

    fun nastaliq(size: Int, lineHeight: Int) = TextStyle(
        fontFamily = NastaliqFamily,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
    )

    return Typography(
        displaySmall = nastaliq(34, 64),
        headlineMedium = nastaliq(26, 52),
        headlineSmall = nastaliq(22, 44),
        titleLarge = nastaliq(20, 40),
        titleMedium = nastaliq(16, 32),
        titleSmall = nastaliq(14, 28),
        bodyLarge = nastaliq(16, 34),
        bodyMedium = nastaliq(14, 30),
        bodySmall = nastaliq(12, 26),
        labelLarge = nastaliq(14, 28),
        labelMedium = nastaliq(12, 24),
        labelSmall = nastaliq(11, 22),
    )
}
