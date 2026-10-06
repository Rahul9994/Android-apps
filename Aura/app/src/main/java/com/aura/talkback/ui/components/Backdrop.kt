package com.aura.talkback.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.aura.talkback.ui.theme.A
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Deep-space aurora: three slow colour fields that drift and breathe, plus a twinkling star field. */
@Composable
fun AuroraBackground(clock: FloatState, accent: Color, modifier: Modifier = Modifier) {
    val accentState = animateColorAsState(accent, tween(1400), label = "bgAccent")
    val stars = remember {
        val rnd = Random(7)
        Array(4) { FloatArray(80) { rnd.nextFloat() } } // x, y, phase, size
    }
    Canvas(modifier.fillMaxSize()) {
        val t = clock.floatValue * 0.22f
        val w = size.width
        val h = size.height
        drawRect(A.Void)

        fun field(cx: Float, cy: Float, radius: Float, color: Color) {
            val p = Offset(cx, cy)
            drawCircle(Brush.radialGradient(listOf(color, Color.Transparent), p, radius), radius, p)
        }
        val ac = accentState.value
        field(w * (0.18f + 0.22f * cos(t)), h * (0.10f + 0.07f * sin(t * 1.7f)), w * 1.05f, ac.copy(alpha = 0.22f))
        field(w * (0.95f + 0.12f * sin(t * 0.8f)), h * (0.36f + 0.10f * cos(t * 1.1f)), w * 0.85f, A.Magenta.copy(alpha = 0.10f))
        field(w * (0.30f + 0.28f * sin(t * 0.6f + 1.4f)), h * (0.95f + 0.05f * cos(t * 1.9f)), w * 0.95f, A.Cyan.copy(alpha = 0.08f))

        val time = clock.floatValue
        val (xs, ys, ph, sz) = stars
        for (i in xs.indices) {
            val a = 0.10f + 0.32f * (0.5f + 0.5f * sin(time * (0.6f + ph[i]) + ph[i] * 6.28f))
            drawCircle(Color.White.copy(alpha = a), (0.6f + sz[i] * 1.1f) * density, Offset(xs[i] * w, ys[i] * h))
        }
    }
}
