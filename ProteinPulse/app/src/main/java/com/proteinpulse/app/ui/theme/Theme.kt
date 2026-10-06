package com.proteinpulse.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Protein Pulse palette: deep ink background, a mint → aqua → violet signature gradient. */
object PP {
    val Ink = Color(0xFF090A13)
    val Ink2 = Color(0xFF11132A)
    val Ink3 = Color(0xFF191C38)
    val Text = Color(0xFFF3F5FF)
    val TextDim = Color(0xFFA3A9CD)
    val TextMute = Color(0xFF666B92)
    val Mint = Color(0xFF6EF2A4)
    val Aqua = Color(0xFF35D6EE)
    val Violet = Color(0xFF8F7CFF)
    val Amber = Color(0xFFFFB547)
    val Rose = Color(0xFFFF6B8A)

    val brand = Brush.linearGradient(listOf(Mint, Aqua, Violet))
    val brandHorizontal = Brush.horizontalGradient(listOf(Mint, Aqua, Violet))

    /** Colour for a protein amount: low → rose, medium → amber, solid → aqua, big → mint. */
    fun proteinColor(grams: Double): Color = when {
        grams >= 30 -> Mint
        grams >= 15 -> Aqua
        grams >= 7 -> Amber
        else -> Rose
    }
}

private val Display = FontFamily.SansSerif

private val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 64.sp, letterSpacing = (-2.5).sp, lineHeight = 66.sp),
    displayMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontSize = 44.sp, letterSpacing = (-1.5).sp, lineHeight = 48.sp),
    headlineLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, letterSpacing = (-1).sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = (-0.4).sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 19.sp, letterSpacing = (-0.2).sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 0.4.sp),
    labelSmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.2.sp),
)

private val AppColors = darkColorScheme(
    primary = PP.Mint,
    onPrimary = PP.Ink,
    secondary = PP.Aqua,
    onSecondary = PP.Ink,
    tertiary = PP.Violet,
    background = PP.Ink,
    onBackground = PP.Text,
    surface = PP.Ink2,
    onSurface = PP.Text,
    surfaceVariant = PP.Ink3,
    onSurfaceVariant = PP.TextDim,
    surfaceContainerHigh = PP.Ink3,
    surfaceContainerHighest = PP.Ink3,
    outline = PP.TextMute,
    error = PP.Rose,
)

@Composable
fun ProteinPulseTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AppColors, typography = AppTypography, content = content)
}
