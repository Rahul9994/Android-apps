package com.aura.talkback.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.talkback.ai.Brain
import com.aura.talkback.data.AppInfo
import com.aura.talkback.data.AuraSettings
import com.aura.talkback.ui.MainViewModel
import com.aura.talkback.ui.components.AppIcon
import com.aura.talkback.ui.components.AuraSwitch
import com.aura.talkback.ui.components.Chip
import com.aura.talkback.ui.components.Gap
import com.aura.talkback.ui.components.GhostButton
import com.aura.talkback.ui.components.Glass
import com.aura.talkback.ui.components.HGap
import com.aura.talkback.ui.components.StatusPill
import com.aura.talkback.ui.motion.enter
import com.aura.talkback.ui.motion.pressable
import com.aura.talkback.ui.overlay.formatMinutes
import com.aura.talkback.ui.screenPadding
import com.aura.talkback.ui.theme.A
import kotlinx.coroutines.delay

@Composable
fun AppsScreen(vm: MainViewModel, s: AuraSettings) {
    val apps by vm.apps.collectAsStateWithLifecycle()
    val usage by vm.usage.collectAsStateWithLifecycle()
    val notifs by vm.notificationCounts.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }

    val minutes = remember(usage) { usage?.apps?.associate { it.pkg to it.ms } ?: emptyMap() }
    val shown = remember(apps, s.watched, minutes, query) {
        apps.filter { query.isBlank() || it.label.contains(query, ignoreCase = true) }
            .sortedWith(compareByDescending<AppInfo> { it.pkg in s.watched }.thenByDescending { minutes[it.pkg] ?: 0L })
    }
    val now by produceState(System.currentTimeMillis()) { while (true) { value = System.currentTimeMillis(); delay(1000) } }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = screenPadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "header") {
            Column(Modifier.enter(0)) {
                Text("APPS", style = MaterialTheme.typography.labelSmall, color = A.TextMute)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Who I watch", style = MaterialTheme.typography.headlineLarge, color = A.Text, modifier = Modifier.weight(1f))
                    StatusPill("${s.watched.size} watched", A.Cyan)
                }
                Gap(6.dp)
                Text("Switch on the apps that eat your time. Tap an app to lock it right now.", style = MaterialTheme.typography.bodyMedium, color = A.TextDim)
            }
        }
        item(key = "search") {
            Row(
                Modifier
                    .enter(1)
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, null, tint = A.TextMute, modifier = Modifier.size(20.dp))
                HGap(10.dp)
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = A.Text),
                    cursorBrush = SolidColor(A.Cyan),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        Box { if (query.isEmpty()) Text("Search apps", style = MaterialTheme.typography.bodyLarge, color = A.TextMute); inner() }
                    },
                )
            }
        }
        items(shown, key = { it.pkg }) { app ->
            val watched = app.pkg in s.watched
            val lockedUntil = s.locks[app.pkg]?.takeIf { it > now }
            val open = expanded == app.pkg
            Glass(
                Modifier.animateItem().fillMaxWidth().pressable { expanded = if (open) null else app.pkg },
                shape = RoundedCornerShape(24.dp),
                padding = PaddingValues(14.dp),
                glow = if (watched) A.Iris else null,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(app.pkg, 44.dp)
                    HGap(12.dp)
                    Column(Modifier.weight(1f)) {
                        Text(app.label, style = MaterialTheme.typography.titleMedium, color = A.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val mins = minutes[app.pkg]?.let { Brain.minutes(it) } ?: 0
                        val pings = notifs[app.pkg] ?: 0
                        Text(
                            buildString {
                                append(if (mins > 0) "${formatMinutes(mins)} today" else "Not opened today")
                                if (pings > 0) append(" · $pings pings")
                            },
                            style = MaterialTheme.typography.labelMedium, color = A.TextMute,
                        )
                    }
                    if (lockedUntil != null) {
                        StatusPill("%d:%02d".format((lockedUntil - now) / 60_000, ((lockedUntil - now) / 1000) % 60), A.Magenta, icon = Icons.Rounded.Lock)
                        HGap(8.dp)
                    }
                    AuraSwitch(watched, { vm.setWatched(app.pkg, it) })
                }
                AnimatedVisibility(open, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    Column(Modifier.padding(top = 14.dp)) {
                        if (lockedUntil != null) {
                            GhostButton("Unlock ${app.label}", { vm.unlock(app.pkg); expanded = null }, Modifier.fillMaxWidth(), icon = Icons.Rounded.LockOpen)
                        } else {
                            Text("LOCK NOW FOR", style = MaterialTheme.typography.labelSmall, color = A.TextMute)
                            Gap(8.dp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(15, 30, 60, 120).forEach { m ->
                                    Chip(
                                        formatMinutes(m),
                                        selected = false,
                                        onClick = { vm.lock(app.pkg, m); expanded = null },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 12.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
