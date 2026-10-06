package com.proteinpulse.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.proteinpulse.app.ui.theme.PP
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The hero: a glowing gradient ring that sweeps to today's progress, with an orbiting
 * dashed halo, a bright comet tip, and an amber second lap once you pass the goal.
 */
@Composable
fun ProteinRing(consumed: Double, goal: Int, modifier: Modifier = Modifier, size: Dp = 252.dp) {
    val target = if (goal > 0) (consumed / goal).toFloat() else 0f
    val progress = remember { Animatable(0f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(target) {
        if (first) {
            first = false
            progress.animateTo(target, tween(1600, delayMillis = 250, easing = FastOutSlowInEasing))
        } else {
            progress.animateTo(target, spring(dampingRatio = 0.62f, stiffness = 45f))
        }
    }

    val infinite = rememberInfiniteTransition(label = "ring")
    val orbit by infinite.animateFloat(
        0f, 360f, infiniteRepeatable(tween(30_000, easing = LinearEasing)), label = "orbit",
    )
    val pulse by infinite.animateFloat(
        0.55f, 1f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse",
    )

    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 20.dp.toPx()
            val inset = stroke / 2 + 16.dp.toPx()
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            val radius = arcSize.width / 2

            rotate(orbit) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.10f),
                    radius = this.size.minDimension / 2 - 3.dp.toPx(),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 12.dp.toPx())),
                        cap = StrokeCap.Round,
                    ),
                )
            }
            // Inner hairline
            drawCircle(Color.White.copy(alpha = 0.05f), radius - stroke * 1.1f, style = Stroke(1.dp.toPx()))
            // Track
            drawArc(Color.White.copy(alpha = 0.06f), 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))

            val p = progress.value.coerceIn(0f, 1f)
            val sweep = 360f * p
            if (p > 0.002f) {
                val brush = Brush.sweepGradient(
                    0f to PP.Mint, 0.45f to PP.Aqua, 0.85f to PP.Violet, 1f to PP.Mint,
                    center = center,
                )
                rotate(-90f) {
                    // Fake bloom: wide, faint strokes underneath the main arc.
                    for (i in 3 downTo 1) {
                        drawArc(
                            brush, 0f, sweep, false, topLeft, arcSize,
                            alpha = 0.07f * pulse,
                            style = Stroke(stroke + i * 9.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }
                    drawArc(brush, 0f, sweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                val angle = Math.toRadians((sweep - 90f).toDouble())
                val tip = center + Offset((cos(angle) * radius).toFloat(), (sin(angle) * radius).toFloat())
                drawCircle(Color.White.copy(alpha = 0.18f * pulse), stroke * 1.05f, tip)
                drawCircle(Color.White, stroke * 0.30f, tip)
            }

            val over = (progress.value - 1f).coerceIn(0f, 1f)
            if (over > 0f) {
                val lapInset = stroke * 1.25f
                rotate(-90f) {
                    drawArc(
                        PP.Amber, 0f, 360f * over, false,
                        topLeft + Offset(lapInset, lapInset),
                        Size(arcSize.width - lapInset * 2, arcSize.height - lapInset * 2),
                        style = Stroke(stroke * 0.28f, cap = StrokeCap.Round),
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            RollingNumber(
                value = consumed.roundToInt(),
                style = MaterialTheme.typography.displayLarge,
                suffix = "g",
            )
            Text("of $goal g protein", style = MaterialTheme.typography.bodyMedium, color = PP.TextDim)
            Gap(10.dp)
            val pct = if (goal > 0) (consumed / goal * 100).roundToInt() else 0
            Pill(
                text = if (pct >= 100) "Goal hit · $pct%" else "$pct%",
                color = if (pct >= 100) PP.Amber else PP.Mint,
            )
        }
    }
}
