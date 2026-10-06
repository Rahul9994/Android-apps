package com.aura.talkback.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.talkback.data.AiProvider
import com.aura.talkback.data.AuraSettings
import com.aura.talkback.ui.Async
import com.aura.talkback.ui.MainViewModel
import com.aura.talkback.ui.components.AuraSwitch
import com.aura.talkback.ui.components.AuroraButton
import com.aura.talkback.ui.components.Gap
import com.aura.talkback.ui.components.GhostButton
import com.aura.talkback.ui.components.Glass
import com.aura.talkback.ui.components.HGap
import com.aura.talkback.ui.components.Label
import com.aura.talkback.ui.components.StatusPill
import com.aura.talkback.ui.motion.Springs
import com.aura.talkback.ui.motion.enter
import com.aura.talkback.ui.motion.pressable
import com.aura.talkback.ui.overlay.formatMinutes
import com.aura.talkback.ui.screenPadding
import com.aura.talkback.ui.theme.A
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, s: AuraSettings) {
    val perms by vm.permissions.collectAsStateWithLifecycle()
    val memories by vm.memories.collectAsStateWithLifecycle()
    var showModels by remember { mutableStateOf(false) }
    var showMemory by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = screenPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(Modifier.enter(0)) {
                Text("YOU", style = MaterialTheme.typography.labelSmall, color = A.TextMute)
                Text("Make Aura yours", style = MaterialTheme.typography.headlineLarge, color = A.Text)
            }
        }
        item { ProfileCard(vm, s, Modifier.enter(1)) }
        item { RulesCard(vm, s, Modifier.enter(2)) }
        item { AiCard(vm, s, Modifier.enter(3)) { showModels = true } }
        item {
            Glass(Modifier.fillMaxWidth().enter(4)) {
                Label("Permissions") { if (perms.all) StatusPill("All on", A.Lime) }
                Gap(14.dp)
                PermissionList(perms, vm)
            }
        }
        item {
            Glass(Modifier.fillMaxWidth().enter(5).pressable { showMemory = true }, padding = PaddingValues(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Psychology, null, tint = A.Iris)
                    HGap(12.dp)
                    Column(Modifier.weight(1f)) {
                        Text("Memory", style = MaterialTheme.typography.titleMedium, color = A.Text)
                        Text("Aura remembers ${memories.size} thing${if (memories.size == 1) "" else "s"} about you", style = MaterialTheme.typography.bodyMedium, color = A.TextDim)
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = A.TextMute)
                }
            }
        }
        item {
            Row(Modifier.padding(horizontal = 6.dp).enter(6), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Lock, null, tint = A.TextMute, modifier = Modifier.size(16.dp))
                Text(
                    "Aura only sees app names and times — never your screen, messages or notification text. Your key, memories and history stay on this phone. When the AI is on, short summaries (app, minutes, your goals) are sent to the provider you chose.",
                    style = MaterialTheme.typography.bodyMedium, color = A.TextMute,
                )
            }
        }
    }

    if (showModels) {
        ModalBottomSheet(
            onDismissRequest = { showModels = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = A.Night2,
        ) { ModelPicker(vm, s) { showModels = false } }
    }
    if (showMemory) {
        ModalBottomSheet(
            onDismissRequest = { showMemory = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = A.Night2,
        ) { MemorySheet(memories, vm::forget) }
    }
}

@Composable
private fun ProfileCard(vm: MainViewModel, s: AuraSettings, modifier: Modifier) {
    var name by rememberSaveable { mutableStateOf(s.userName) }
    Glass(modifier.fillMaxWidth()) {
        Label("Profile")
        Gap(12.dp)
        Field(name, { name = it; vm.update { st -> st.copy(userName = it) } }, "Your name")
        Gap(18.dp)
        Text("HOW I TALK TO YOU", style = MaterialTheme.typography.labelSmall, color = A.TextMute)
        Gap(10.dp)
        ToneCards(s.tone, { t -> vm.update { it.copy(tone = t) } })
    }
}

@Composable
private fun RulesCard(vm: MainViewModel, s: AuraSettings, modifier: Modifier) {
    Glass(modifier.fillMaxWidth(), glow = A.Iris) {
        Label("Talk-back rules")
        Gap(12.dp)
        ToggleRow("Aura is watching", "Pause to stop all talk-backs", s.monitoring) { v -> vm.update { it.copy(monitoring = v) } }
        Gap(8.dp)
        ToggleRow("Speak out loud", "Reads talk-backs aloud on your media volume — works on vibrate too", s.voice, vm::setVoice)
        GhostButton("Test voice", vm::testVoice, Modifier.padding(top = 4.dp), icon = Icons.Rounded.RecordVoiceOver, color = A.Cyan)
        Gap(16.dp)
        ValueSlider("Talk back after", s.nudgeAfterMin, 5..90, 5, { formatMinutes(it) }) { v -> vm.update { it.copy(nudgeAfterMin = v) } }
        Gap(8.dp)
        Text("LATE NIGHT", style = MaterialTheme.typography.labelSmall, color = A.Magenta)
        Gap(8.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            HourStepper("From", s.nightStartHour, Modifier.weight(1f)) { h -> vm.update { it.copy(nightStartHour = h) } }
            HGap(10.dp)
            HourStepper("Until", s.nightEndHour, Modifier.weight(1f)) { h -> vm.update { it.copy(nightEndHour = h) } }
        }
        Gap(12.dp)
        ValueSlider("At night, talk back after", s.nightNudgeAfterMin, 0..30, 1, { if (it == 0) "Instantly" else formatMinutes(it) }) { v ->
            vm.update { it.copy(nightNudgeAfterMin = v) }
        }
        ValueSlider("Default lock length", s.defaultLockMin, 10..180, 5, { formatMinutes(it) }) { v -> vm.update { it.copy(defaultLockMin = v) } }
        Gap(14.dp)
        AuroraButton("Preview the talk-back", vm::previewTalkBack, Modifier.fillMaxWidth(), icon = Icons.Rounded.AutoAwesome)
    }
}

@Composable
private fun AiCard(vm: MainViewModel, s: AuraSettings, modifier: Modifier, onPickModel: () -> Unit) {
    val test by vm.test.collectAsStateWithLifecycle()
    val current by vm.currentModel.collectAsStateWithLifecycle()
    val uri = LocalUriHandler.current
    var draft by rememberSaveable(s.provider) { mutableStateOf(s.activeKey) }
    var show by remember { mutableStateOf(false) }
    val dirty = draft.trim() != s.activeKey
    LaunchedEffect(s.provider, s.activeKey, s.chosenModel) { vm.refreshCurrentModel() }

    Glass(modifier.fillMaxWidth(), glow = A.Cyan) {
        Label("AI engine") { if (s.hasAi) StatusPill("Connected key", A.Lime) else StatusPill("No key", A.Amber) }
        Gap(14.dp)
        ProviderToggle(s.provider) { p -> vm.resetAiStatus(); vm.update { it.copy(provider = p) } }
        Gap(14.dp)
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Key, null, tint = A.TextDim, modifier = Modifier.size(18.dp))
            HGap(10.dp)
            BasicTextField(
                value = draft,
                onValueChange = { draft = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = A.Text),
                cursorBrush = SolidColor(A.Cyan),
                visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box { if (draft.isEmpty()) Text("${s.provider.label} API key", style = MaterialTheme.typography.bodyLarge, color = A.TextMute); inner() }
                },
            )
            Icon(
                if (show) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, "Show key", tint = A.TextMute,
                modifier = Modifier.size(20.dp).pressable { show = !show },
            )
        }
        Gap(8.dp)
        Row(Modifier.pressable { uri.openUri(s.provider.keyUrl) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Get a key at ${s.provider.keyUrl.removePrefix("https://")}", style = MaterialTheme.typography.labelLarge, color = A.Cyan)
            HGap(4.dp)
            Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, tint = A.Cyan, modifier = Modifier.size(14.dp))
        }
        Gap(10.dp)
        // Model row
        Row(
            Modifier
                .fillMaxWidth()
                .pressable(enabled = s.hasAi, onClick = onPickModel)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Model", style = MaterialTheme.typography.labelMedium, color = A.TextMute)
                Text(
                    if (s.chosenModel.isBlank()) "Auto · ${current.ifBlank { "best available" }}" else s.chosenModel,
                    style = MaterialTheme.typography.titleMedium, color = A.Text, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = A.TextMute)
        }
        Gap(14.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AuroraButton(
                if (dirty) "Save key" else "Saved",
                {
                    vm.resetAiStatus()
                    vm.update { st -> if (st.provider == AiProvider.GROQ) st.copy(groqKey = draft) else st.copy(openRouterKey = draft) }
                },
                Modifier.weight(1f), enabled = dirty,
            )
            GhostButton("Test", { vm.testAi() }, Modifier.weight(1f))
        }
        AnimatedContent(test, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "test") { t ->
            when (t) {
                is Async.Ready -> StatusLine(t.value, Icons.Rounded.CheckCircle, A.Lime)
                is Async.Failed -> StatusLine(t.message, Icons.Rounded.ErrorOutline, A.Red)
                Async.Loading -> StatusLine("Testing…", Icons.Rounded.Refresh, A.TextDim)
                else -> Box(Modifier)
            }
        }
    }
}

@Composable
private fun StatusLine(text: String, icon: ImageVector, color: Color) {
    Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        HGap(8.dp)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

@Composable
private fun ProviderToggle(selected: AiProvider, onSelect: (AiProvider) -> Unit) {
    val options = AiProvider.entries
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.06f))
            .padding(4.dp)
    ) {
        val w = maxWidth / options.size
        val x by animateDpAsState(w * selected.ordinal, Springs.snappy(), label = "provider")
        Box(Modifier.offset(x = x).width(w).fillMaxHeight().clip(CircleShape).background(A.auroraH))
        Row(Modifier.fillMaxSize()) {
            options.forEach { p ->
                Box(Modifier.weight(1f).fillMaxHeight().pressable { onSelect(p) }, contentAlignment = Alignment.Center) {
                    Text(p.label, style = MaterialTheme.typography.labelLarge, color = if (p == selected) A.Void else A.TextDim)
                }
            }
        }
    }
}

@Composable
private fun ModelPicker(vm: MainViewModel, s: AuraSettings, onClose: () -> Unit) {
    val models by vm.models.collectAsStateWithLifecycle()
    val current by vm.currentModel.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { vm.loadModels() }
    fun choose(id: String) {
        vm.update { st -> if (st.provider == AiProvider.GROQ) st.copy(groqModel = id) else st.copy(openRouterModel = id) }
        vm.resetAiStatus()
        onClose()
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 30.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${s.provider.label} models", style = MaterialTheme.typography.headlineSmall, color = A.Text, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.Refresh, "Refresh", tint = A.TextDim, modifier = Modifier.size(22.dp).pressable { vm.loadModels(refresh = true) })
        }
        Gap(12.dp)
        ModelRow("Auto · best available", "Currently ${current.ifBlank { s.provider.preferredModels.first() }}. Switches by itself if a model is retired.", s.chosenModel.isBlank(), true) { choose("") }
        Gap(12.dp)
        Row(
            Modifier.fillMaxWidth().clip(CircleShape).background(Color.White.copy(alpha = 0.06f)).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Search, null, tint = A.TextMute, modifier = Modifier.size(18.dp))
            HGap(8.dp)
            BasicTextField(
                query, { query = it }, singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = A.Text), cursorBrush = SolidColor(A.Cyan),
                modifier = Modifier.weight(1f),
                decorationBox = { inner -> Box { if (query.isEmpty()) Text("Search models", style = MaterialTheme.typography.bodyLarge, color = A.TextMute); inner() } },
            )
        }
        Gap(10.dp)
        when (val m = models) {
            Async.Loading, Async.Idle -> Text("Loading models…", style = MaterialTheme.typography.bodyMedium, color = A.TextMute, modifier = Modifier.padding(12.dp))
            is Async.Failed -> Text(m.message, style = MaterialTheme.typography.bodyMedium, color = A.Red, modifier = Modifier.padding(12.dp))
            is Async.Ready -> {
                val list = m.value.filter { query.isBlank() || it.contains(query, ignoreCase = true) }
                val top = s.provider.preferredModels.toSet()
                LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(list, key = { it }) { id ->
                        ModelRow(id, if (id in top) "Top pick" else null, s.chosenModel == id, id in top) { choose(id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelRow(title: String, subtitle: String?, selected: Boolean, highlight: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .pressable(onClick = onClick)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) A.Iris.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.04f))
            .border(1.dp, if (selected) A.Iris.copy(alpha = 0.6f) else Color.Transparent, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = A.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.labelMedium, color = if (highlight) A.Cyan else A.TextMute)
        }
        if (selected) Icon(Icons.Rounded.CheckCircle, null, tint = A.Iris)
    }
}

@Composable
private fun Field(value: String, onValue: (String) -> Unit, hint: String) {
    BasicTextField(
        value = value,
        onValueChange = onValue,
        singleLine = true,
        textStyle = MaterialTheme.typography.titleLarge.copy(color = A.Text),
        cursorBrush = SolidColor(A.Cyan),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
            .padding(16.dp),
        decorationBox = { inner -> Box { if (value.isEmpty()) Text(hint, style = MaterialTheme.typography.titleLarge, color = A.TextMute); inner() } },
    )
}

@Composable
private fun ToggleRow(title: String, body: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = A.Text)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = A.TextDim)
        }
        HGap(12.dp)
        AuraSwitch(checked, onChange)
    }
}

@Composable
private fun ValueSlider(title: String, value: Int, range: IntRange, step: Int, format: (Int) -> String, onDone: (Int) -> Unit) {
    var v by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = A.Text, modifier = Modifier.weight(1f))
            Text(format(v.roundToInt()), style = MaterialTheme.typography.titleMedium, color = A.Cyan)
        }
        Slider(
            value = v,
            onValueChange = { v = ((it / step).roundToInt() * step).toFloat().coerceIn(range.first.toFloat(), range.last.toFloat()) },
            onValueChangeFinished = { onDone(v.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = A.Text,
                activeTrackColor = A.Iris,
                inactiveTrackColor = Color.White.copy(alpha = 0.10f),
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun HourStepper(label: String, hour: Int, modifier: Modifier, onChange: (Int) -> Unit) {
    val text = when { hour == 0 -> "12 AM"; hour < 12 -> "$hour AM"; hour == 12 -> "12 PM"; else -> "${hour - 12} PM" }
    Row(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton(Icons.Rounded.Remove) { onChange((hour + 23) % 24) }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = A.TextMute)
            AnimatedContent(text, label = "hour") { Text(it, style = MaterialTheme.typography.titleMedium, color = A.Text) }
        }
        StepButton(Icons.Rounded.Add) { onChange((hour + 1) % 24) }
    }
}

@Composable
private fun StepButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier.size(36.dp).pressable(scaleDown = 0.85f, onClick = onClick).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = A.Text, modifier = Modifier.size(18.dp)) }
}
