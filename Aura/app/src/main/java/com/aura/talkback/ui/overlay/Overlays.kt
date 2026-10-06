package com.aura.talkback.ui.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SweepGradientShader
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.talkback.ui.components.AppIcon
import com.aura.talkback.ui.components.AuraOrb
import com.aura.talkback.ui.components.AuroraButton
import com.aura.talkback.ui.components.Chip
import com.aura.talkback.ui.components.Gap
import com.aura.talkback.ui.components.GhostButton
import com.aura.talkback.ui.components.HGap
import com.aura.talkback.ui.components.OrbMood
import com.aura.talkback.ui.components.Ring
import com.aura.talkback.ui.components.StatusPill
import com.aura.talkback.ui.components.WordReveal
import com.aura.talkback.ui.motion.LocalFrameClock
import com.aura.talkback.ui.motion.rememberHaptics
import com.aura.talkback.ui.theme.A
import com.aura.talkback.ui.theme.Sora
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlin.math.sin

data class NudgeUi(
    val pkg: String,
    val label: String,
    val sessionMin: Int,
    val message: String?,
    val lockMinutes: Int,
    val night: Boolean,
    val voiceHint: String? = null,
)

fun formatMinutes(m: Int) = when {
    m % 60 == 0 && m >= 60 -> "${m / 60} hr"
    m > 60 -> "${m / 60}h ${m % 60}m"
    else -> "$m min"
}

/** A sweep gradient whose angle can be rotated — used for the living border. */
fun rotatingSweep(center: Offset, colors: List<Color>, degrees: Float): Brush = object : ShaderBrush() {
    override fun createShader(size: Size): Shader =
        SweepGradientShader(center, colors).apply {
            setLocalMatrix(android.graphics.Matrix().apply { setRotate(degrees, center.x, center.y) })
        }
}

@Composable
private fun OverlayExit(visible: MutableTransitionState<Boolean>, onGone: () -> Unit) {
    LaunchedEffect(visible) {
        snapshotFlow { visible.isIdle && !visible.currentState && !visible.targetState }.first { it }
        onGone()
    }
}

/** The talk-back: slides up over whatever app you're in. */
@Composable
fun NudgeOverlay(
    visible: MutableTransitionState<Boolean>,
    ui: StateFlow<NudgeUi>,
    speaking: StateFlow<Boolean>,
    onLock: (Int) -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
    onGone: () -> Unit,
) {
    val state by ui.collectAsState()
    val isSpeaking by speaking.collectAsState()
    val clock = LocalFrameClock.current
    val haptics = rememberHaptics()
    OverlayExit(visible, onGone)
    LaunchedEffect(Unit) { delay(120); haptics.confirm() }

    val transition = rememberTransition(visible, label = "nudge")
    val scrim by transition.animateFloat({ tween(420) }, label = "scrim") { if (it) 1f else 0f }
    val card by transition.animateFloat(
        { if (targetState) spring(dampingRatio = 0.78f, stiffness = 230f) else tween(260) }, label = "card",
    ) { if (it) 1f else 0f }

    var lockChoice by remember { mutableIntStateOf(0) }
    val chosen = if (lockChoice > 0) lockChoice else state.lockMinutes
    val options = remember(state.lockMinutes) { (listOf(15, 30, 60, 120) + state.lockMinutes).distinct().sorted() }
    val mood = when {
        state.message == null -> OrbMood.THINKING
        isSpeaking -> OrbMood.SPEAKING
        else -> OrbMood.LISTENING
    }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = scrim }
                .drawBehind {
                    drawRect(Brush.verticalGradient(listOf(A.Void.copy(alpha = 0.45f), A.Void.copy(alpha = 0.92f))))
                    val t = clock.floatValue
                    val p = Offset(size.width / 2f, size.height * (0.98f - 0.02f * sin(t * 0.8f)))
                    val r = size.width * (0.95f + 0.05f * sin(t * 1.3f))
                    drawCircle(Brush.radialGradient(listOf(A.Iris.copy(alpha = 0.34f), A.Magenta.copy(alpha = 0.10f), Color.Transparent), p, r), r, p)
                }
        )

        // Overlay windows often get no nav-bar inset, so always keep clear of the gesture bar.
        val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = maxOf(navInset, 30.dp) + 8.dp)
                .graphicsLayer {
                    translationY = (1f - card) * 420.dp.toPx()
                    alpha = card.coerceIn(0f, 1f)
                    val s = 0.92f + 0.08f * card
                    scaleX = s; scaleY = s
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                }
                .clip(RoundedCornerShape(38.dp))
                .drawWithContent {
                    drawRect(A.Night.copy(alpha = 0.98f))
                    val glowP = Offset(size.width * 0.15f, 0f)
                    drawCircle(Brush.radialGradient(listOf(A.Iris.copy(alpha = 0.22f), Color.Transparent), glowP, size.width * 0.7f), size.width * 0.7f, glowP)
                    drawContent()
                    val deg = clock.floatValue * 70f
                    drawRoundRect(
                        rotatingSweep(center, listOf(A.Cyan, A.Iris, A.Magenta, Color.Transparent, A.Cyan), deg),
                        cornerRadius = CornerRadius(38.dp.toPx()),
                        style = Stroke(1.6.dp.toPx()),
                    )
                }
                .padding(22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AuraOrb(clock, Modifier.size(68.dp), mood)
                HGap(12.dp)
                Column(Modifier.weight(1f)) {
                    Text("Aura", style = MaterialTheme.typography.titleLarge.merge(TextStyle(brush = A.auroraH)))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(state.pkg, 18.dp)
                        HGap(6.dp)
                        Text("${state.label} · ${state.sessionMin} min", style = MaterialTheme.typography.labelMedium, color = A.TextDim)
                    }
                }
                if (state.night) StatusPill("Late night", A.Magenta, icon = Icons.Rounded.Bedtime)
            }
            Gap(18.dp)
            AnimatedContent(
                targetState = state.message,
                contentKey = { it == null },
                transitionSpec = { (fadeIn(tween(250)) + scaleIn(initialScale = 0.98f)) togetherWith fadeOut(tween(120)) },
                label = "message",
            ) { msg ->
                if (msg == null) ThinkingLines()
                else WordReveal(
                    msg,
                    style = TextStyle(fontFamily = Sora, fontSize = 21.sp, lineHeight = 29.sp, letterSpacing = (-0.3).sp),
                    color = A.Text,
                    msPerWord = 70,
                )
            }
            state.voiceHint?.let {
                Gap(10.dp)
                Text("🔇 $it", style = MaterialTheme.typography.labelMedium, color = A.Amber)
            }
            Gap(20.dp)
            Text("LOCK FOR", style = MaterialTheme.typography.labelSmall, color = A.TextMute)
            Gap(8.dp)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { m ->
                    Chip(formatMinutes(m), selected = m == chosen, onClick = { lockChoice = m; haptics.tick() })
                }
            }
            Gap(16.dp)
            AuroraButton(
                "Lock ${state.label} · ${formatMinutes(chosen)}",
                onClick = { haptics.confirm(); onLock(chosen) },
                icon = Icons.Rounded.Lock,
                modifier = Modifier.fillMaxWidth(),
            )
            Gap(10.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton("5 more min", onSnooze, Modifier.weight(1f), icon = Icons.Rounded.Timer)
                GhostButton("I'm good", onDismiss, Modifier.weight(1f), color = A.TextDim)
            }
        }
    }
}

@Composable
private fun ThinkingLines() {
    val clock = LocalFrameClock.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(1f, 0.92f, 0.6f).forEachIndexed { i, w ->
            Box(
                Modifier
                    .fillMaxWidth(w)
                    .height(16.dp)
                    .clip(CircleShape)
                    .drawBehind {
                        drawRect(Color.White.copy(alpha = 0.06f))
                        val x = (((clock.floatValue * 0.7f) + i * 0.15f) % 1.4f - 0.2f) * size.width
                        drawRect(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, A.Iris.copy(alpha = 0.45f), A.Cyan.copy(alpha = 0.3f), Color.Transparent),
                                startX = x - size.width * 0.3f, endX = x + size.width * 0.1f,
                            )
                        )
                    }
            )
        }
    }
}

/** Shown when you open an app Aura locked: you're sent home, and it explains why. */
@Composable
fun LockOverlay(
    visible: MutableTransitionState<Boolean>,
    pkg: String,
    appLabel: String,
    until: Long,
    reminder: String?,
    name: String,
    onClose: () -> Unit,
    onGone: () -> Unit,
) {
    val clock = LocalFrameClock.current
    val haptics = rememberHaptics()
    OverlayExit(visible, onGone)
    LaunchedEffect(Unit) { haptics.reject(); delay(6500); onClose() }

    val remaining by produceState(until - System.currentTimeMillis(), until) {
        while (true) { value = (until - System.currentTimeMillis()).coerceAtLeast(0); delay(200) }
    }
    val transition = rememberTransition(visible, label = "lock")
    val enter by transition.animateFloat(
        { if (targetState) spring(dampingRatio = 0.7f, stiffness = 200f) else tween(240) }, label = "enter",
    ) { if (it) 1f else 0f }

    val mins = (remaining / 60_000).toInt()
    val secs = ((remaining / 1000) % 60).toInt()

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = enter.coerceIn(0f, 1f) }
            .drawBehind {
                drawRect(A.Void.copy(alpha = 0.94f))
                val t = clock.floatValue
                val p = Offset(size.width / 2, size.height * 0.4f)
                val r = size.width * (0.8f + 0.06f * sin(t * 1.2f))
                drawCircle(Brush.radialGradient(listOf(A.Magenta.copy(alpha = 0.18f), A.Iris.copy(alpha = 0.08f), Color.Transparent), p, r), r, p)
            }
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                val s = 0.86f + 0.14f * enter
                scaleX = s; scaleY = s
                translationY = (1f - enter) * 60.dp.toPx()
            },
        ) {
            Ring(
                progress = (remaining % 60_000) / 60_000f,
                size = 230.dp,
                stroke = 6.dp,
                colors = listOf(A.Magenta, A.Iris, A.Cyan, A.Magenta),
            ) {
                AuraOrb(clock, Modifier.size(150.dp), OrbMood.ALERT)
            }
            Gap(22.dp)
            Text(
                "%d:%02d".format(mins, secs),
                style = MaterialTheme.typography.displayLarge.merge(TextStyle(brush = A.auroraH)),
            )
            Text("until you can open it", style = MaterialTheme.typography.labelMedium, color = A.TextMute)
            Gap(22.dp)
            Row(
                Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                    .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(pkg, 28.dp)
                HGap(8.dp)
                Icon(Icons.Rounded.Lock, null, tint = A.Magenta, modifier = Modifier.size(16.dp))
                HGap(4.dp)
                Text("$appLabel is locked", style = MaterialTheme.typography.labelLarge, color = A.Text)
            }
            Gap(16.dp)
            Text(
                reminder?.let { "$name, remember: $it" } ?: "$name, you asked me to keep you away from it. I'm keeping my word.",
                style = MaterialTheme.typography.bodyLarge,
                color = A.TextDim,
                textAlign = TextAlign.Center,
            )
            Gap(28.dp)
            GhostButton("Okay", onClose, Modifier.fillMaxWidth(0.6f))
        }
    }
}
