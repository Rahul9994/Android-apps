package com.proteinpulse.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.proteinpulse.app.ui.theme.PP

/** An odometer: each digit rolls up (or down) independently when the value changes. */
@Composable
fun RollingNumber(
    value: Int,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = PP.Text,
    suffix: String? = null,
) {
    var previous by remember { mutableIntStateOf(value) }
    val goingUp = value >= previous
    SideEffect { previous = value }

    val text = value.toString()
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        text.forEachIndexed { i, ch ->
            // Key by position from the right so units stay units when the digit count changes.
            key(text.length - i) {
                AnimatedContent(
                    targetState = ch,
                    transitionSpec = {
                        val spec = spring<IntOffset>(dampingRatio = 0.7f, stiffness = 380f)
                        if (goingUp) {
                            (slideInVertically(spec) { it } + fadeIn()) togetherWith (slideOutVertically(spec) { -it } + fadeOut())
                        } else {
                            (slideInVertically(spec) { -it } + fadeIn()) togetherWith (slideOutVertically(spec) { it } + fadeOut())
                        } using SizeTransform(clip = true)
                    },
                    label = "digit",
                ) { c ->
                    Text(c.toString(), style = style, color = color)
                }
            }
        }
        if (suffix != null) {
            Text(
                suffix,
                style = style.copy(fontSize = style.fontSize * 0.42f),
                color = PP.TextDim,
                modifier = Modifier.padding(start = 3.dp, bottom = 10.dp),
            )
        }
    }
}
