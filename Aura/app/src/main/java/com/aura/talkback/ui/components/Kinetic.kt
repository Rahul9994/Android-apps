package com.aura.talkback.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.aura.talkback.ui.motion.LocalFrameClock
import com.aura.talkback.ui.motion.pressable
import com.aura.talkback.ui.theme.A
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/** Odometer digits that roll independently when the value changes. */
@Composable
fun RollingNumber(value: Int, style: TextStyle, modifier: Modifier = Modifier, color: Color = A.Text) {
    var previous by remember { mutableIntStateOf(value) }
    val up = value >= previous
    SideEffect { previous = value }
    val text = value.toString()
    Row(modifier) {
        text.forEachIndexed { i, ch ->
            key(text.length - i) {
                AnimatedContent(
                    targetState = ch,
                    transitionSpec = {
                        val s = spring<IntOffset>(dampingRatio = 0.72f, stiffness = 420f)
                        val spec = if (up) (slideInVertically(s) { it } + fadeIn()) togetherWith (slideOutVertically(s) { -it } + fadeOut())
                        else (slideInVertically(s) { -it } + fadeIn()) togetherWith (slideOutVertically(s) { it } + fadeOut())
                        spec.using(SizeTransform(clip = true))
                    },
                    label = "digit",
                ) { c -> Text(c.toString(), style = style, color = color) }
            }
        }
    }
}

/** Gradient progress ring with a comet head; sweeps in on first show and springs on change. */
@Composable
fun Ring(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    stroke: Dp = 10.dp,
    colors: List<Color> = listOf(A.Cyan, A.Iris, A.Magenta, A.Cyan),
    content: @Composable () -> Unit = {},
) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(progress) { p.animateTo(progress.coerceIn(0f, 1f), spring(dampingRatio = 0.85f, stiffness = 40f)) }
    val clock = LocalFrameClock.current
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = stroke.toPx()
            val inset = sw / 2 + 4.dp.toPx()
            val arc = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val tl = Offset(inset, inset)
            drawArc(Color.White.copy(alpha = 0.07f), 0f, 360f, false, tl, arc, style = Stroke(sw))
            val v = p.value
            if (v > 0.003f) {
                val sweep = 360f * v
                rotate(-90f) {
                    val brush = Brush.sweepGradient(colors, center)
                    drawArc(brush, 0f, sweep, false, tl, arc, alpha = 0.18f, style = Stroke(sw * 2.4f, cap = StrokeCap.Round))
                    drawArc(brush, 0f, sweep, false, tl, arc, style = Stroke(sw, cap = StrokeCap.Round))
                }
                val a = Math.toRadians((sweep - 90f).toDouble())
                val rr = arc.width / 2
                val head = center + Offset((cos(a) * rr).toFloat(), (sin(a) * rr).toFloat())
                val pulse = 0.6f + 0.4f * sin(clock.floatValue * 3f)
                drawCircle(Color.White.copy(alpha = 0.22f * pulse), sw * 1.1f, head)
                drawCircle(Color.White, sw * 0.28f, head)
            }
        }
        content()
    }
}

/**
 * AI text that materialises word by word: each word fades, rises and sharpens in turn.
 * Per-frame work happens in graphicsLayer blocks, so the reveal doesn't recompose.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordReveal(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = A.Text,
    msPerWord: Int = 55,
    startDelayMs: Long = 0,
    centered: Boolean = false,
    onDone: () -> Unit = {},
) {
    val words = remember(text) { text.split(' ').filter { it.isNotEmpty() } }
    val progress = remember(text) { Animatable(0f) }
    LaunchedEffect(text) {
        delay(startDelayMs)
        progress.animateTo(words.size + 1f, tween(words.size * msPerWord + 400, easing = LinearEasing))
        onDone()
    }
    FlowRow(
        modifier,
        horizontalArrangement = if (centered) androidx.compose.foundation.layout.Arrangement.Center
        else androidx.compose.foundation.layout.Arrangement.Start,
    ) {
        words.forEachIndexed { i, w ->
            Text(
                "$w ",
                style = style,
                color = color,
                modifier = Modifier.graphicsLayer {
                    val local = ((progress.value - i) / 1.4f).coerceIn(0f, 1f)
                    val eased = FastOutSlowInEasing.transform(local)
                    alpha = eased
                    translationY = (1f - eased) * 10.dp.toPx()
                },
            )
        }
    }
}

data class NavItem(val label: String, val icon: ImageVector)

/**
 * Floating glass nav bar. The indicator is a liquid capsule: its leading edge springs fast and its
 * trailing edge lags behind, so it stretches toward the new tab and snaps back into shape.
 */
@Composable
fun LiquidNavBar(items: List<NavItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val left = remember { Animatable(selected.toFloat()) }
    val right = remember { Animatable(selected + 1f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(selected) {
        val target = selected.toFloat()
        val movingRight = target > left.value
        val fast = spring<Float>(dampingRatio = 0.78f, stiffness = 700f)
        val slow = spring<Float>(dampingRatio = 0.82f, stiffness = 220f)
        scope.launch { left.animateTo(target, if (movingRight) slow else fast) }
        scope.launch { right.animateTo(target + 1f, if (movingRight) fast else slow) }
    }
    val shape = RoundedCornerShape(36.dp)
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(shape)
            .drawBehind {
                drawRect(A.Night.copy(alpha = 0.97f))
                drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.06f), Color.Transparent)))
            }
            .drawBehind {
                val slot = size.width / items.size
                val pad = 7.dp.toPx()
                val l = left.value * slot + pad
                val r = right.value * slot - pad
                val top = pad
                val h = size.height - pad * 2
                drawRoundRect(
                    Brush.horizontalGradient(listOf(A.Cyan.copy(alpha = 0.24f), A.Iris.copy(alpha = 0.30f), A.Magenta.copy(alpha = 0.22f)), l, r),
                    Offset(l, top), Size(r - l, h), CornerRadius(h / 2),
                )
                drawRoundRect(
                    Color.White.copy(alpha = 0.10f), Offset(l, top), Size(r - l, h), CornerRadius(h / 2),
                    style = Stroke(1.dp.toPx()),
                )
            }
    ) {
        Row(Modifier.fillMaxSize()) {
            items.forEachIndexed { i, item ->
                val isSel = i == selected
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pressable(scaleDown = 0.9f) { onSelect(i) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                ) {
                    val tint = if (isSel) A.Text else A.TextMute
                    Icon(
                        item.icon, item.label, tint = tint,
                        modifier = Modifier.size(23.dp).graphicsLayer {
                            val near = 1f - kotlin.math.abs((left.value + right.value - 1f) / 2f - i).coerceIn(0f, 1f)
                            val s = 1f + 0.14f * near
                            scaleX = s; scaleY = s
                        },
                    )
                    Text(item.label, style = MaterialTheme.typography.labelMedium, color = tint, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}

/** 24 hourly bars of distracting-app minutes; late-night hours glow magenta. */
@Composable
fun HourBars(minutesByHour: FloatArray, nightStart: Int, nightEnd: Int, modifier: Modifier = Modifier) {
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }
    val now = java.time.LocalTime.now().hour
    Canvas(modifier) {
        val max = maxOf(30f, minutesByHour.maxOrNull() ?: 0f)
        val slot = size.width / 24f
        val barW = slot * 0.56f
        for (h in 0 until 24) {
            val night = if (nightStart <= nightEnd) h in nightStart until nightEnd else h >= nightStart || h < nightEnd
            val stagger = ((grow.value * 1.6f) - h / 24f * 0.6f).coerceIn(0f, 1f)
            val v = minutesByHour[h] / max * stagger
            val x = h * slot + (slot - barW) / 2
            drawRoundRect(Color.White.copy(alpha = if (h == now) 0.10f else 0.045f), Offset(x, 0f), Size(barW, size.height), CornerRadius(barW / 2))
            val bh = (size.height * v).coerceAtLeast(if (minutesByHour[h] > 0) barW else 0f)
            if (bh > 0f) {
                val colors = if (night) listOf(A.Magenta, A.Iris) else listOf(A.Cyan, A.Iris)
                drawRoundRect(
                    Brush.verticalGradient(colors, size.height - bh, size.height),
                    Offset(x, size.height - bh), Size(barW, bh), CornerRadius(barW / 2),
                )
            }
        }
    }
}
