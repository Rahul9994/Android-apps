package com.proteinpulse.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.proteinpulse.app.ui.theme.PP
import kotlin.random.Random

private class Particle(
    val vx: Float, val vy: Float, val w: Float, val h: Float,
    val spin: Float, val rot0: Float, val color: Color, val startX: Float,
)

/** Confetti that bursts from the top third of the screen whenever [trigger] changes (and isn't 0). */
@Composable
fun ConfettiBurst(trigger: Int, modifier: Modifier = Modifier) {
    if (trigger == 0) return
    val colors = listOf(PP.Mint, PP.Aqua, PP.Violet, PP.Amber, PP.Rose, Color.White)
    val particles = remember(trigger) {
        List(110) {
            Particle(
                vx = Random.nextFloat() * 700f - 350f,
                vy = -(Random.nextFloat() * 900f + 450f),
                w = Random.nextFloat() * 5f + 5f,
                h = Random.nextFloat() * 8f + 8f,
                spin = Random.nextFloat() * 720f - 360f,
                rot0 = Random.nextFloat() * 360f,
                color = colors.random(),
                startX = Random.nextFloat() * 0.4f + 0.3f,
            )
        }
    }
    var time by remember(trigger) { mutableFloatStateOf(0f) }
    val duration = 3.2f
    LaunchedEffect(trigger) {
        val start = withFrameNanos { it }
        while (time < duration) {
            time = (withFrameNanos { it } - start) / 1_000_000_000f
        }
    }
    if (time >= duration) return

    Canvas(modifier.fillMaxSize()) {
        val gravity = 1500f
        val fade = (1f - ((time - duration + 0.8f) / 0.8f)).coerceIn(0f, 1f)
        particles.forEach { p ->
            val x = size.width * p.startX + p.vx * density * time * 0.6f
            val y = size.height * 0.32f + (p.vy * time + 0.5f * gravity * time * time) * density * 0.6f
            rotate(p.rot0 + p.spin * time, pivot = Offset(x, y)) {
                drawRect(
                    color = p.color.copy(alpha = fade),
                    topLeft = Offset(x - p.w * density / 2, y - p.h * density / 2),
                    size = Size(p.w * density, p.h * density),
                )
            }
        }
    }
}

/** Three bouncing gradient dots plus shimmering text, shown while the AI works. */
@Composable
fun AiThinking(text: String, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "thinking")
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(PP.Mint, PP.Aqua, PP.Violet).forEachIndexed { i, c ->
                val lift by transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        tween(420, easing = LinearEasing),
                        RepeatMode.Reverse,
                        StartOffset(i * 140),
                    ),
                    label = "dot$i",
                )
                Box(
                    Modifier
                        .size(9.dp)
                        .graphicsLayer {
                            translationY = -lift * 7.dp.toPx()
                            scaleX = 0.8f + lift * 0.3f
                            scaleY = 0.8f + lift * 0.3f
                        }
                        .clip(CircleShape)
                        .background(c)
                )
            }
        }
        val shift by transition.animateFloat(
            0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "textShift",
        )
        Text(
            text,
            style = MaterialTheme.typography.titleMedium.merge(
                TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(PP.TextDim, Color.White, PP.TextDim),
                        start = Offset(-400f + shift * 1200f, 0f),
                        end = Offset(shift * 1200f, 0f),
                    )
                )
            ),
        )
    }
}
