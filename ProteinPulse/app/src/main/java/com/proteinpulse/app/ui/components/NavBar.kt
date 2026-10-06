package com.proteinpulse.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.proteinpulse.app.ui.Tab
import com.proteinpulse.app.ui.theme.PP

private data class NavItem(val tab: Tab, val label: String, val icon: ImageVector)

private val items = listOf(
    NavItem(Tab.TODAY, "Today", Icons.Rounded.Today),
    NavItem(Tab.COACH, "Coach", Icons.Rounded.AutoAwesome),
    NavItem(Tab.REPORT, "Report", Icons.Rounded.Insights),
    NavItem(Tab.SETTINGS, "Settings", Icons.Rounded.Tune),
)

/** Floating glass nav bar with a gradient pill that springs between tabs. */
@Composable
fun FloatingNavBar(selected: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(34.dp)
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(70.dp)
            .clip(shape)
            .background(PP.Ink2)
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.04f))), shape)
    ) {
        val itemWidth = maxWidth / items.size
        val indicatorX by animateDpAsState(
            targetValue = itemWidth * selected.ordinal,
            animationSpec = spring(dampingRatio = 0.68f, stiffness = 420f),
            label = "indicator",
        )
        Box(
            Modifier
                .offset(x = indicatorX)
                .width(itemWidth)
                .fillMaxHeight()
                .padding(7.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.linearGradient(listOf(PP.Mint.copy(alpha = 0.22f), PP.Violet.copy(alpha = 0.18f))))
                .border(1.dp, PP.Mint.copy(alpha = 0.25f), RoundedCornerShape(28.dp))
        )
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly) {
            items.forEach { item ->
                val isSelected = item.tab == selected
                val tint by animateColorAsState(if (isSelected) PP.Mint else PP.TextMute, label = "tint")
                val scale by animateFloatAsState(
                    if (isSelected) 1.12f else 1f,
                    spring(dampingRatio = 0.4f, stiffness = 500f),
                    label = "iconScale",
                )
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .bouncyClickable { onSelect(item.tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        tint = tint,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer { scaleX = scale; scaleY = scale },
                    )
                    Text(item.label, style = MaterialTheme.typography.labelMedium, color = tint)
                }
            }
        }
    }
}
