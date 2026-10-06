package com.aura.talkback.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aura.talkback.AuraApp
import com.aura.talkback.ui.motion.LocalFrameClock
import com.aura.talkback.ui.motion.Springs
import com.aura.talkback.ui.motion.pressable
import com.aura.talkback.ui.theme.A
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Frosted surface: layered translucent gradient, hairline top highlight, optional coloured glow. */
@Composable
fun Glass(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    glow: Color? = null,
    padding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .clip(shape)
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.075f), Color.White.copy(alpha = 0.022f))))
                if (glow != null) {
                    val p = Offset(size.width * 0.12f, 0f)
                    drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = 0.22f), Color.Transparent), p, size.width * 0.8f), size.width * 0.8f, p)
                }
            }
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.17f), Color.White.copy(alpha = 0.035f))), shape)
            .padding(padding),
        content = content,
    )
}

@Composable
fun Label(text: String, modifier: Modifier = Modifier, color: Color = A.TextMute, trailing: (@Composable () -> Unit)? = null) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = color, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

/** The hero button: aurora capsule with a light sweep gliding across it. */
@Composable
fun AuroraButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    loading: Boolean = false,
    enabled: Boolean = true,
) {
    val clock = LocalFrameClock.current
    Box(
        modifier
            .heightIn(min = 56.dp)
            .pressable(enabled = enabled && !loading, onClick = onClick)
            .clip(CircleShape)
            .drawWithContent {
                if (enabled) {
                    drawRect(A.auroraH)
                    val x = ((clock.floatValue * 0.45f) % 1.6f - 0.3f) * size.width
                    drawRect(
                        Brush.linearGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.38f), Color.Transparent),
                            start = Offset(x - size.width * 0.25f, 0f), end = Offset(x + size.width * 0.05f, size.height),
                        )
                    )
                } else {
                    drawRect(Color.White.copy(alpha = 0.07f))
                }
                drawContent()
            }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (loading) CircularProgressIndicator(Modifier.size(18.dp), color = A.Void, strokeWidth = 2.dp)
            else if (icon != null) Icon(icon, null, tint = if (enabled) A.Void else A.TextMute, modifier = Modifier.size(20.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, color = if (enabled) A.Void else A.TextMute)
        }
    }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, color: Color = A.Text) {
    Row(
        modifier
            .heightIn(min = 52.dp)
            .pressable(onClick = onClick)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

@Composable
fun Chip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
) {
    val bg by animateColorAsState(if (selected) A.Iris.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f), label = "chipBg")
    val fg by animateColorAsState(if (selected) A.Text else A.TextDim, label = "chipFg")
    val border by animateColorAsState(if (selected) A.Iris.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.10f), label = "chipBorder")
    Row(
        modifier
            .pressable(onClick = onClick)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, border, CircleShape)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) Icon(icon, null, tint = fg, modifier = Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = fg, maxLines = 1, softWrap = false)
    }
}

@Composable
fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(
        modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.13f))
            .border(1.dp, color.copy(alpha = 0.3f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = color, modifier = Modifier.size(13.dp))
        else Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

/** Spring-loaded switch; the track fills with aurora when on. */
@Composable
fun AuraSwitch(checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val x by animateDpAsState(if (checked) 22.dp else 0.dp, Springs.bouncy(), label = "thumb")
    val track by animateColorAsState(if (checked) Color.Transparent else Color.White.copy(alpha = 0.10f), label = "track")
    Box(
        modifier
            .width(54.dp)
            .height(32.dp)
            .pressable { onChange(!checked) }
            .clip(CircleShape)
            .drawBehind {
                if (checked) drawRect(A.auroraH)
                drawRect(track)
            }
            .padding(4.dp),
    ) {
        Box(
            Modifier
                .offset(x = x)
                .size(24.dp)
                .clip(CircleShape)
                .background(if (checked) A.Void else A.TextDim)
        )
    }
}

@Composable
fun IconBadge(icon: ImageVector, color: Color, size: Dp = 40.dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(Brush.linearGradient(listOf(color.copy(alpha = 0.32f), color.copy(alpha = 0.08f))))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(size * 0.32f)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = color, modifier = Modifier.size(size * 0.5f)) }
}

/** The real app icon, loaded off the main thread. */
@Composable
fun AppIcon(pkg: String, size: Dp = 40.dp, modifier: Modifier = Modifier) {
    val bitmap by produceState<ImageBitmap?>(null, pkg) {
        value = withContext(Dispatchers.IO) { AuraApp.instance.catalog.icon(pkg) }
    }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        AnimatedContent(
            targetState = bitmap,
            transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.8f)) togetherWith (fadeOut() + scaleOut()) },
            label = "icon",
        ) { bmp ->
            if (bmp != null) Image(bmp, null, Modifier.size(size).clip(RoundedCornerShape(size * 0.28f)))
            else Box(Modifier.size(size).clip(RoundedCornerShape(size * 0.28f)).background(Color.White.copy(alpha = 0.06f)))
        }
    }
}

@Composable
fun Gap(h: Dp) = Spacer(Modifier.height(h))

@Composable
fun HGap(w: Dp) = Spacer(Modifier.width(w))
