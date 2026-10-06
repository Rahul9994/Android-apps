package com.proteinpulse.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.proteinpulse.app.ui.theme.PP
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Slowly drifting colour fields behind every screen. [accent] tints the scene per tab. */
@Composable
fun AnimatedBackground(accent: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "bg")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(26_000, easing = LinearEasing)),
        label = "bgT",
    )
    Canvas(modifier.fillMaxSize()) {
        drawRect(PP.Ink)
        val w = size.width
        val h = size.height
        fun blob(cx: Float, cy: Float, radius: Float, color: Color) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color, color.copy(alpha = 0f)),
                    center = Offset(cx, cy),
                    radius = radius,
                ),
                radius = radius,
                center = Offset(cx, cy),
            )
        }
        blob(w * (0.15f + 0.18f * cos(t)), h * (0.12f + 0.06f * sin(2 * t)), w * 0.95f, accent.copy(alpha = 0.20f))
        blob(w * (0.95f + 0.12f * sin(t)), h * (0.38f + 0.10f * cos(t)), w * 0.8f, PP.Violet.copy(alpha = 0.13f))
        blob(w * (0.35f + 0.25f * sin(t + 1.6f)), h * (0.92f + 0.05f * cos(2 * t)), w * 0.9f, PP.Aqua.copy(alpha = 0.09f))
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    padding: PaddingValues = PaddingValues(20.dp),
    tint: Color = Color.White,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(tint.copy(alpha = 0.085f), tint.copy(alpha = 0.03f))))
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.03f))), shape)
            .padding(padding),
        content = content,
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = PP.TextMute, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(
        modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.28f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

/** Primary call-to-action: gradient capsule with a springy press. */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    loading: Boolean = false,
    enabled: Boolean = true,
) {
    Box(
        modifier
            .bouncyClickable(enabled = enabled && !loading, onClick = onClick)
            .clip(CircleShape)
            .background(if (enabled) PP.brandHorizontal else Brush.horizontalGradient(listOf(PP.Ink3, PP.Ink3)))
            .padding(horizontal = 22.dp, vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (loading) {
                CircularProgressIndicator(color = PP.Ink, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            } else if (icon != null) {
                Icon(icon, null, tint = if (enabled) PP.Ink else PP.TextMute, modifier = Modifier.size(20.dp))
            }
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) PP.Ink else PP.TextMute,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Gradient-filled bar that grows with a spring. */
@Composable
fun ProgressBar(progress: Float, modifier: Modifier = Modifier, height: Dp = 10.dp, color: Brush = PP.brandHorizontal) {
    val animated by androidx.compose.animation.core.animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 60f),
        label = "bar",
    )
    Box(
        modifier
            .height(height)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.07f))
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
fun Gap(size: Dp) = Spacer(Modifier.height(size).width(size))
