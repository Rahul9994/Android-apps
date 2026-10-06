package com.aura.talkback.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aura.talkback.R

/** Aura palette: near-black void, an aurora of cyan → iris → magenta, and soft signal colours. */
object A {
    val Void = Color(0xFF05060B)
    val Night = Color(0xFF0C0E1B)
    val Night2 = Color(0xFF141731)
    val Text = Color(0xFFF5F6FF)
    val TextDim = Color(0xFFA9AED3)
    val TextMute = Color(0xFF656A93)
    val Iris = Color(0xFF8B7BFF)
    val Cyan = Color(0xFF3EE6FF)
    val Magenta = Color(0xFFFF5FD2)
    val Lime = Color(0xFFB4FF72)
    val Amber = Color(0xFFFFC266)
    val Red = Color(0xFFFF5C7C)

    val aurora = listOf(Cyan, Iris, Magenta)
    val auroraBrush = Brush.linearGradient(aurora)
    val auroraH = Brush.horizontalGradient(aurora)
}

@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int, weight: Int) =
    Font(res, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

val Sora = FontFamily(
    variable(R.font.sora, 400), variable(R.font.sora, 500), variable(R.font.sora, 600),
    variable(R.font.sora, 700), variable(R.font.sora, 800),
)
val Inter = FontFamily(
    variable(R.font.inter, 400), variable(R.font.inter, 500), variable(R.font.inter, 600), variable(R.font.inter, 700),
)

private val AuraType = Typography(
    displayLarge = TextStyle(fontFamily = Sora, fontWeight = FontWeight.ExtraBold, fontSize = 56.sp, lineHeight = 58.sp, letterSpacing = (-2.4).sp),
    displayMedium = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = (-1.6).sp),
    headlineLarge = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-1).sp),
    headlineSmall = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.2.sp),
    labelSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 10.5.sp, letterSpacing = 1.6.sp),
)

private val AuraColors = darkColorScheme(
    primary = A.Iris, onPrimary = A.Void, secondary = A.Cyan, tertiary = A.Magenta,
    background = A.Void, onBackground = A.Text, surface = A.Night, onSurface = A.Text,
    surfaceVariant = A.Night2, onSurfaceVariant = A.TextDim,
    surfaceContainer = A.Night, surfaceContainerHigh = A.Night2, surfaceContainerHighest = A.Night2,
    surfaceContainerLow = A.Night, outline = A.TextMute, error = A.Red,
)

@Composable
fun AuraTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AuraColors, typography = AuraType, content = content)
}
