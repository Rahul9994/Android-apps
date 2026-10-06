package com.aura.talkback.ui.motion

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Shared spring curves so every surface moves with the same physics. */
object Springs {
    fun <T> snappy() = spring<T>(dampingRatio = 0.82f, stiffness = 520f)
    fun <T> gentle() = spring<T>(dampingRatio = 0.9f, stiffness = 170f)
    fun <T> bouncy() = spring<T>(dampingRatio = 0.55f, stiffness = 380f)
    fun <T> soft() = spring<T>(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow)
}

/**
 * Seconds since first frame, ticking on every display frame (144 Hz on 144 Hz panels).
 * Read `.floatValue` only inside draw lambdas so a frame redraws without recomposing.
 */
@Composable
fun rememberFrameClock(): FloatState {
    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { time.floatValue = (it - start) / 1_000_000_000f }
    }
    return time
}

val LocalFrameClock = compositionLocalOf<FloatState> { mutableFloatStateOf(0f) }

class Haptics(private val view: android.view.View) {
    fun tick() = view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    fun click() = view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    fun confirm() = view.performHapticFeedback(
        if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS
    )
    fun reject() = view.performHapticFeedback(
        if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
    )
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    return remember(view) { Haptics(view) }
}

/** Press-to-squash with a haptic tick. No ripple: the motion is the feedback. */
@Composable
fun Modifier.pressable(enabled: Boolean = true, scaleDown: Float = 0.955f, haptic: Boolean = true, onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptics = rememberHaptics()
    val scale by animateFloatAsState(if (pressed && enabled) scaleDown else 1f, Springs.bouncy(), label = "press")
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interaction, indication = null, enabled = enabled) {
            if (haptic) haptics.click()
            onClick()
        }
}

/** Fade + rise + settle, staggered by [index]. Pure graphicsLayer, so it never re-lays out. */
@Composable
fun Modifier.enter(index: Int = 0, delayMs: Long = 0, distance: Float = 36f): Modifier {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMs + index.coerceAtMost(10) * 55L)
        p.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = 210f))
    }
    return graphicsLayer {
        val v = p.value
        alpha = v.coerceIn(0f, 1f)
        translationY = (1f - v) * distance.dp.toPx()
        val s = 0.965f + 0.035f * v
        scaleX = s; scaleY = s
    }
}
