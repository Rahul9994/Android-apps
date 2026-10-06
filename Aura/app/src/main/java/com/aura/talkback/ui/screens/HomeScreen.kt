package com.aura.talkback.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.talkback.ai.Brain
import com.aura.talkback.data.AuraSettings
import com.aura.talkback.data.DayUsage
import com.aura.talkback.data.Nudge
import com.aura.talkback.data.Outcome
import com.aura.talkback.service.Foreground
import com.aura.talkback.ui.Async
import com.aura.talkback.ui.MainViewModel
import com.aura.talkback.ui.Tab
import com.aura.talkback.ui.components.AppIcon
import com.aura.talkback.ui.components.AuraOrb
import com.aura.talkback.ui.components.AuroraButton
import com.aura.talkback.ui.components.Gap
import com.aura.talkback.ui.components.GhostButton
import com.aura.talkback.ui.components.Glass
import com.aura.talkback.ui.components.HGap
import com.aura.talkback.ui.components.HourBars
import com.aura.talkback.ui.components.Label
import com.aura.talkback.ui.components.OrbMood
import com.aura.talkback.ui.components.Ring
import com.aura.talkback.ui.components.RollingNumber
import com.aura.talkback.ui.components.StatusPill
import com.aura.talkback.ui.components.WordReveal
import com.aura.talkback.ui.motion.LocalFrameClock
import com.aura.talkback.ui.motion.enter
import com.aura.talkback.ui.motion.pressable
import com.aura.talkback.ui.overlay.formatMinutes
import com.aura.talkback.ui.screenPadding
import com.aura.talkback.ui.theme.A
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(vm: MainViewModel, s: AuraSettings) {
    val perms by vm.permissions.collectAsStateWithLifecycle()
    val serviceOn by vm.serviceOn.collectAsStateWithLifecycle()
    val fg by vm.foreground.collectAsStateWithLifecycle()
    val usage by vm.usage.collectAsStateWithLifecycle()
    val nudges by vm.nudgesToday.collectAsStateWithLifecycle()
    val insight by vm.insight.collectAsStateWithLifecycle()
    val notifs by vm.notificationCounts.collectAsStateWithLifecycle()

    LaunchedEffect(usage != null, s.hasAi) { vm.loadInsight() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = screenPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "header") { Header(s, serviceOn && perms.coreReady, Modifier.enter(0)) { vm.tab.value = Tab.YOU } }
        item(key = "hero") { Hero(vm, s, fg, serviceOn, perms.coreReady, Modifier.enter(1)) }
        item(key = "today") { TodayCard(usage, nudges, notifs.values.sum(), Modifier.enter(2)) }
        val locks = s.activeLocks()
        if (locks.isNotEmpty()) item(key = "locks") { LocksCard(locks, vm, Modifier.enter(3)) }
        item(key = "insight") { InsightCard(insight, s.hasAi, Modifier.enter(4), onRefresh = { vm.loadInsight(force = true) }) { vm.tab.value = Tab.YOU } }
        item(key = "rhythm") { RhythmCard(usage, s, Modifier.enter(5)) }
        if (nudges.isNotEmpty()) {
            item(key = "nudgesLabel") { Label("Talk-backs today", Modifier.padding(top = 6.dp).enter(6)) }
            items(nudges, key = { it.id }) { NudgeRow(it, Modifier.animateItem()) }
        }
        val top = usage?.apps.orEmpty().take(6)
        if (top.isNotEmpty()) {
            item(key = "topLabel") { Label("Most used today", Modifier.padding(top = 6.dp).enter(7)) }
            item(key = "top") { TopApps(top.map { Triple(it.pkg, it.label, it.ms) }, s.watched, Modifier.enter(8)) }
        }
    }
}

@Composable
private fun Header(s: AuraSettings, live: Boolean, modifier: Modifier, onSettings: () -> Unit) {
    val hour = LocalTime.now().hour
    val greet = when (hour) { in 5..11 -> "Morning"; in 12..16 -> "Afternoon"; in 17..21 -> "Evening"; else -> "Up late" }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)).uppercase(),
                style = MaterialTheme.typography.labelSmall, color = A.TextMute,
            )
            Text("$greet, ${s.displayName}", style = MaterialTheme.typography.headlineLarge, color = A.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        HGap(10.dp)
        Box(Modifier.pressable(onClick = onSettings)) {
            if (live && s.monitoring) StatusPill("Live", A.Lime) else StatusPill(if (!s.monitoring) "Paused" else "Setup", A.Amber, icon = Icons.Rounded.Settings)
        }
    }
}

@Composable
private fun Hero(vm: MainViewModel, s: AuraSettings, fg: Foreground?, serviceOn: Boolean, permsReady: Boolean, modifier: Modifier) {
    val clock = LocalFrameClock.current
    val now by produceState(System.currentTimeMillis()) { while (true) { value = System.currentTimeMillis(); delay(1000) } }
    val watchingApp = fg?.takeIf { it.pkg in s.watched }
    val night = s.isNight(LocalTime.now().hour)
    val limit = if (night) s.nightNudgeAfterMin else s.nudgeAfterMin

    Glass(modifier.fillMaxWidth(), glow = if (watchingApp != null) A.Magenta else A.Iris, padding = PaddingValues(vertical = 24.dp, horizontal = 20.dp)) {
        val sessionMs = watchingApp?.let { now - it.since } ?: 0L
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val progress = if (watchingApp != null && limit > 0) (sessionMs / (limit * 60_000f)).coerceIn(0f, 1f) else 0f
            Ring(progress, size = 236.dp, stroke = 5.dp, colors = listOf(A.Cyan, A.Iris, A.Magenta, A.Cyan)) {
                AuraOrb(
                    clock, Modifier.size(190.dp),
                    when {
                        !permsReady || !serviceOn -> OrbMood.CALM
                        watchingApp != null && progress > 0.75f -> OrbMood.ALERT
                        watchingApp != null -> OrbMood.LISTENING
                        else -> OrbMood.CALM
                    },
                )
            }
        }
        Gap(18.dp)
        val key = when {
            !permsReady || !serviceOn -> "setup"
            !s.monitoring -> "paused"
            watchingApp != null -> "watching:${watchingApp.pkg}"
            else -> "quiet"
        }
        AnimatedContent(
            targetState = key,
            transitionSpec = { (fadeIn(tween(400)) + slideInVertically(spring(dampingRatio = 0.8f, stiffness = 200f)) { it / 3 }) togetherWith fadeOut(tween(150)) },
            label = "heroState",
        ) { k ->
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                when {
                    k == "setup" -> {
                        Text("I can't see your apps yet", style = MaterialTheme.typography.headlineSmall, color = A.Text)
                        Gap(6.dp)
                        Text("Turn on Usage access and Accessibility so I can talk back.", style = MaterialTheme.typography.bodyMedium, color = A.TextDim)
                        Gap(16.dp)
                        AuroraButton("Finish setup", { vm.tab.value = Tab.YOU }, Modifier.fillMaxWidth())
                    }
                    k == "paused" -> {
                        Text("I'm paused", style = MaterialTheme.typography.headlineSmall, color = A.Text)
                        Text("Turn monitoring back on in You → Talk-back rules.", style = MaterialTheme.typography.bodyMedium, color = A.TextDim)
                    }
                    k.startsWith("watching") && watchingApp != null -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppIcon(watchingApp.pkg, 22.dp)
                            HGap(8.dp)
                            Text("You're on ${watchingApp.label}", style = MaterialTheme.typography.titleMedium, color = A.TextDim)
                        }
                        Row(verticalAlignment = Alignment.Bottom) {
                            RollingNumber((sessionMs / 60_000).toInt(), MaterialTheme.typography.displayLarge)
                            Text(" min", style = MaterialTheme.typography.titleLarge, color = A.TextDim, modifier = Modifier.padding(bottom = 10.dp))
                        }
                        Text(
                            if (sessionMs >= limit * 60_000L) "I'm about to say something…" else "I'll speak up at $limit min${if (night) " (late-night rule)" else ""}",
                            style = MaterialTheme.typography.labelMedium, color = A.TextMute,
                        )
                    }
                    else -> {
                        Text("All quiet", style = MaterialTheme.typography.headlineSmall, color = A.Text)
                        Gap(4.dp)
                        Text(
                            "Watching ${s.watched.size} apps · I speak up after ${s.nudgeAfterMin} min, ${s.nightNudgeAfterMin} after ${hourLabel(s.nightStartHour)}",
                            style = MaterialTheme.typography.bodyMedium, color = A.TextDim,
                        )
                    }
                }
            }
        }
    }
}

private fun hourLabel(h: Int) = when { h == 0 -> "12 AM"; h < 12 -> "$h AM"; h == 12 -> "12 PM"; else -> "${h - 12} PM" }

@Composable
private fun TodayCard(usage: DayUsage?, nudges: List<Nudge>, notifications: Int, modifier: Modifier) {
    val screenMin = usage?.let { Brain.minutes(it.totalMs) } ?: 0
    val distractMin = usage?.let { Brain.minutes(it.watchedMs) } ?: 0
    val locked = nudges.count { it.outcome == Outcome.LOCKED }
    val snoozed = nudges.count { it.outcome == Outcome.SNOOZED }
    // Decays smoothly with distracting minutes (1 h ≈ 78, 3 h ≈ 47, 5 h ≈ 29); locking helps, snoozing hurts.
    val focus = (100 * kotlin.math.exp(-distractMin / 240.0) - snoozed * 4 + locked * 5).toInt().coerceIn(5, 100)

    Glass(modifier.fillMaxWidth()) {
        Label("Today")
        Gap(14.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Ring(focus / 100f, size = 124.dp, stroke = 11.dp, colors = listOf(A.Lime, A.Cyan, A.Iris, A.Lime)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    RollingNumber(focus, MaterialTheme.typography.headlineLarge)
                    Text("focus", style = MaterialTheme.typography.labelMedium, color = A.TextMute)
                }
            }
            HGap(18.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Stat(Icons.Rounded.PhoneAndroid, A.Cyan, "Screen time", "${screenMin / 60}h ${screenMin % 60}m")
                Stat(Icons.Rounded.Bolt, A.Magenta, "Distracting", "$distractMin min")
                Stat(Icons.Rounded.TouchApp, A.Iris, "Unlocks", "${usage?.unlocks ?: 0}")
                if (notifications > 0) Stat(Icons.Rounded.AutoAwesome, A.Amber, "Pings", "$notifications")
            }
        }
    }
}

@Composable
private fun Stat(icon: ImageVector, color: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        HGap(8.dp)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = A.TextDim, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium, color = A.Text)
    }
}

@Composable
private fun LocksCard(locks: Map<String, Long>, vm: MainViewModel, modifier: Modifier) {
    val now by produceState(System.currentTimeMillis()) { while (true) { value = System.currentTimeMillis(); delay(1000) } }
    Glass(modifier.fillMaxWidth(), glow = A.Magenta) {
        Label("Locked right now", color = A.Magenta)
        Gap(10.dp)
        locks.entries.sortedBy { it.value }.forEach { (pkg, until) ->
            val left = (until - now).coerceAtLeast(0)
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                AppIcon(pkg, 36.dp)
                HGap(12.dp)
                Column(Modifier.weight(1f)) {
                    Text(com.aura.talkback.AuraApp.instance.catalog.label(pkg), style = MaterialTheme.typography.titleMedium, color = A.Text)
                    Text("%d:%02d left".format(left / 60_000, (left / 1000) % 60), style = MaterialTheme.typography.labelMedium, color = A.Magenta)
                }
                GhostButton("Unlock", { vm.unlock(pkg) }, icon = Icons.Rounded.LockOpen, color = A.TextDim)
            }
        }
    }
}

@Composable
private fun InsightCard(insight: Async<String>, hasAi: Boolean, modifier: Modifier, onRefresh: () -> Unit, onSetup: () -> Unit) {
    val clock = LocalFrameClock.current
    Glass(modifier.fillMaxWidth(), glow = A.Cyan) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AuraOrb(clock, Modifier.size(38.dp), if (insight is Async.Loading) OrbMood.THINKING else OrbMood.CALM)
            HGap(10.dp)
            Label("Aura's read on your day", Modifier.weight(1f))
            if (hasAi) Icon(Icons.Rounded.Refresh, "Refresh", tint = A.TextMute, modifier = Modifier.size(20.dp).pressable(onClick = onRefresh))
        }
        Gap(12.dp)
        AnimatedContent(targetState = insight, contentKey = { it::class }, label = "insight") { st ->
            when {
                !hasAi -> Column {
                    Text("Add a Groq or OpenRouter key and I'll tell you how your day is really going.", style = MaterialTheme.typography.bodyLarge, color = A.TextDim)
                    Gap(12.dp)
                    GhostButton("Add AI key", onSetup)
                }
                st is Async.Ready -> WordReveal(st.value, MaterialTheme.typography.bodyLarge.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize), color = A.Text)
                st is Async.Failed -> Text(st.message, style = MaterialTheme.typography.bodyMedium, color = A.TextMute)
                st is Async.Loading -> Text("Reading your day…", style = MaterialTheme.typography.bodyLarge, color = A.TextMute)
                else -> Text("Use your phone a bit and I'll have thoughts.", style = MaterialTheme.typography.bodyLarge, color = A.TextMute)
            }
        }
    }
}

@Composable
private fun RhythmCard(usage: DayUsage?, s: AuraSettings, modifier: Modifier) {
    Glass(modifier.fillMaxWidth()) {
        Label("Your day's rhythm") {
            StatusPill("Late night", A.Magenta)
        }
        Gap(4.dp)
        Text("Minutes in distracting apps, hour by hour", style = MaterialTheme.typography.bodyMedium, color = A.TextDim)
        Gap(16.dp)
        HourBars(usage?.watchedByHour ?: FloatArray(24), s.nightStartHour, s.nightEndHour, Modifier.fillMaxWidth().height(110.dp))
        Gap(8.dp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("12a", "6a", "12p", "6p", "12a").forEach { Text(it, style = MaterialTheme.typography.labelMedium, color = A.TextMute) }
        }
    }
}

@Composable
private fun NudgeRow(n: Nudge, modifier: Modifier) {
    val (color, label) = when (n.outcome) {
        Outcome.LOCKED -> A.Lime to "Locked ${formatMinutes(n.lockMin)}"
        Outcome.SNOOZED -> A.Amber to "5 more min"
        Outcome.DISMISSED -> A.TextDim to "Brushed off"
        Outcome.PENDING -> A.TextMute to "No answer"
    }
    Glass(modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(n.pkg, 30.dp)
            HGap(10.dp)
            Column(Modifier.weight(1f)) {
                Text("${n.appLabel} · ${n.sessionMin} min", style = MaterialTheme.typography.titleMedium, color = A.Text)
                Text(Brain.timeOf(n.ts), style = MaterialTheme.typography.labelMedium, color = A.TextMute)
            }
            StatusPill(label, color)
        }
        if (n.message.isNotBlank()) {
            Gap(10.dp)
            Text("“${n.message}”", style = MaterialTheme.typography.bodyMedium, color = A.TextDim)
        }
    }
}

@Composable
private fun TopApps(apps: List<Triple<String, String, Long>>, watched: Set<String>, modifier: Modifier) {
    val max = apps.maxOfOrNull { it.third }?.coerceAtLeast(1) ?: 1
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = 60f)) }
    Glass(modifier.fillMaxWidth()) {
        apps.forEachIndexed { i, (pkg, label, ms) ->
            if (i > 0) Gap(14.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(pkg, 34.dp)
                HGap(12.dp)
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(label, style = MaterialTheme.typography.titleMedium, color = A.Text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(formatMinutes(Brain.minutes(ms).coerceAtLeast(1)), style = MaterialTheme.typography.labelLarge, color = A.TextDim)
                    }
                    Gap(6.dp)
                    val frac = ms.toFloat() / max
                    val isWatched = pkg in watched
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.06f))
                            .drawBehind {
                                val w = size.width * frac * grow.value
                                drawRoundRect(
                                    if (isWatched) androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(A.Iris, A.Magenta))
                                    else androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(A.Cyan.copy(alpha = 0.7f), A.Iris.copy(alpha = 0.7f))),
                                    size = Size(w, size.height), cornerRadius = CornerRadius(size.height),
                                )
                            }
                    )
                }
            }
        }
    }
}
