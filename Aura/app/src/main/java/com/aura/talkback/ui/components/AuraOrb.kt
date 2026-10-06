package com.aura.talkback.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.aura.talkback.ui.theme.A
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val POINTS = 72
private val orbLights = listOf(A.Cyan, A.Magenta, A.Iris)

enum class OrbMood(val energy: Float) { CALM(0f), LISTENING(0.35f), THINKING(0.8f), SPEAKING(0.6f), ALERT(1f) }

/**
 * Aura's living presence: a liquid sphere whose outline is a sum of drifting sine waves,
 * filled with a rotating aurora, lit by orbiting colour lights, with a soft bloom.
 * Everything is computed in the draw phase from [clock], so it animates at the display's
 * full refresh rate without recomposition.
 */
@Composable
fun AuraOrb(clock: FloatState, modifier: Modifier = Modifier, mood: OrbMood = OrbMood.CALM) {
    val energy = animateFloatAsState(mood.energy, spring(dampingRatio = 0.8f, stiffness = 60f), label = "orbEnergy")
    val speaking = mood == OrbMood.SPEAKING
    val path = remember { Path() }
    val xs = remember { FloatArray(POINTS) }
    val ys = remember { FloatArray(POINTS) }

    Canvas(modifier) {
        val t = clock.floatValue
        val e = energy.value + if (speaking) 0.18f * (0.5f + 0.5f * sin(t * 11f) * sin(t * 4.3f)) else 0f
        val c = center
        val r0 = size.minDimension / 2f * 0.6f
        val breathe = 1f + 0.03f * sin(t * 1.4f) + 0.04f * e * sin(t * 6f)
        val r = r0 * breathe

        // Bloom
        drawCircle(
            Brush.radialGradient(listOf(A.Iris.copy(alpha = 0.30f + 0.25f * e), Color.Transparent), c, r * 2.1f),
            r * 2.1f, c,
        )
        // Orbiting colour lights behind the sphere
        orbLights.forEachIndexed { i, col ->
            val a = t * (0.35f + 0.18f * i) * (1f + 1.4f * e) + i * 2.094f
            val p = c + Offset(cos(a), sin(a * 1.1f)) * (r * 0.55f)
            drawCircle(Brush.radialGradient(listOf(col.copy(alpha = 0.42f), Color.Transparent), p, r * 1.25f), r * 1.25f, p)
        }

        // Liquid outline
        path.reset()
        val n = POINTS
        val amp1 = 0.035f + 0.06f * e
        val amp2 = 0.022f + 0.045f * e
        for (i in 0 until n) {
            val th = (i.toFloat() / n) * 2f * PI.toFloat()
            val k = 1f + amp1 * sin(3f * th + t * 1.25f) + amp2 * sin(5f * th - t * 1.8f + 1f) + 0.018f * sin(2f * th + t * 0.6f)
            xs[i] = c.x + cos(th) * r * k
            ys[i] = c.y + sin(th) * r * k
        }
        path.moveTo((xs[n - 1] + xs[0]) / 2f, (ys[n - 1] + ys[0]) / 2f)
        for (i in 0 until n) {
            val j = (i + 1) % n
            path.quadraticTo(xs[i], ys[i], (xs[i] + xs[j]) / 2f, (ys[i] + ys[j]) / 2f)
        }
        path.close()

        clipPath(path) {
            rotate(t * 38f * (1f + e), c) {
                drawCircle(
                    Brush.sweepGradient(listOf(A.Cyan, A.Iris, A.Magenta, A.Iris, A.Cyan), c),
                    r * 1.4f, c,
                )
            }
            // Drifting inner light
            val hl = c + Offset(-0.32f * r + 0.12f * r * sin(t * 0.9f), -0.38f * r + 0.1f * r * cos(t * 0.7f))
            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.62f), Color.Transparent), hl, r * 0.75f), r * 0.75f, hl)
            // Depth shading
            val sh = c + Offset(0.35f * r, 0.42f * r)
            drawCircle(Brush.radialGradient(listOf(Color.Transparent, A.Void.copy(alpha = 0.55f)), sh, r * 1.5f), r * 2f, c)
            // Caustic band
            val band = c + Offset(0f, r * (0.25f + 0.08f * sin(t * 1.7f)))
            drawCircle(Brush.radialGradient(listOf(A.Cyan.copy(alpha = 0.35f), Color.Transparent), band, r * 0.55f), r * 0.55f, band)
        }
        drawPath(path, Color.White.copy(alpha = 0.22f + 0.2f * e), style = Stroke(1.2.dp.toPx()))
    }
}
