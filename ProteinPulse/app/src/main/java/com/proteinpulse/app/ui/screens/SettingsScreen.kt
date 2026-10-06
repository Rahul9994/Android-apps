package com.proteinpulse.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.proteinpulse.app.data.AiProvider
import com.proteinpulse.app.data.AppSettings
import com.proteinpulse.app.data.DietType
import com.proteinpulse.app.ui.AppViewModel
import com.proteinpulse.app.ui.Async
import com.proteinpulse.app.ui.components.Gap
import com.proteinpulse.app.ui.components.GlassCard
import com.proteinpulse.app.ui.components.GradientButton
import com.proteinpulse.app.ui.components.RollingNumber
import com.proteinpulse.app.ui.components.SectionLabel
import com.proteinpulse.app.ui.components.bouncyClickable
import com.proteinpulse.app.ui.components.staggeredEntrance
import com.proteinpulse.app.ui.screenPadding
import com.proteinpulse.app.ui.theme.PP
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(vm: AppViewModel, settings: AppSettings) {
    val connection by vm.connection.collectAsStateWithLifecycle()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = screenPadding(bottomExtra = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(Modifier.staggeredEntrance(0)) {
                Text("PREFERENCES", style = MaterialTheme.typography.labelSmall, color = PP.TextMute)
                Text("Settings", style = MaterialTheme.typography.headlineLarge, color = PP.Text)
            }
        }
        item { AiCard(vm, settings, connection, Modifier.staggeredEntrance(1)) }
        item { GoalCard(vm, settings, Modifier.staggeredEntrance(2)) }
        item { DietCard(vm, settings, Modifier.staggeredEntrance(3)) }
        item { ReportTimeCard(vm, settings, Modifier.staggeredEntrance(4)) }
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp).staggeredEntrance(5),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.Lock, null, tint = PP.TextMute, modifier = Modifier.size(16.dp))
                Text(
                    "Your API key and meals are stored only on this phone. Food text is sent to the AI provider you pick, only when you log a meal or ask the coach.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PP.TextMute,
                )
            }
        }
    }
}

@Composable
private fun AiCard(vm: AppViewModel, settings: AppSettings, connection: Async<String>, modifier: Modifier) {
    val provider = settings.provider
    // Local drafts so typing doesn't write to storage on every keystroke.
    var keyDraft by rememberSaveable(provider) { mutableStateOf(settings.activeKey) }
    var modelDraft by rememberSaveable(provider) {
        mutableStateOf(if (provider == AiProvider.GROQ) settings.groqModel else settings.openRouterModel)
    }
    var showKey by remember { mutableStateOf(false) }
    val uri = LocalUriHandler.current
    val dirty = keyDraft.trim() != settings.activeKey ||
        modelDraft.trim() != (if (provider == AiProvider.GROQ) settings.groqModel else settings.openRouterModel)

    GlassCard(modifier.fillMaxWidth()) {
        SectionLabel("AI engine")
        Gap(12.dp)
        ProviderToggle(provider) { p -> vm.updateSettings { it.copy(provider = p) } }
        Gap(16.dp)
        OutlinedTextField(
            value = keyDraft,
            onValueChange = { keyDraft = it },
            label = { Text("${provider.label} API key") },
            leadingIcon = { Icon(Icons.Rounded.Key, null) },
            trailingIcon = {
                Icon(
                    if (showKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    if (showKey) "Hide key" else "Show key",
                    modifier = Modifier.bouncyClickable { showKey = !showKey },
                )
            },
            singleLine = true,
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(18.dp),
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        Gap(10.dp)
        OutlinedTextField(
            value = modelDraft,
            onValueChange = { modelDraft = it },
            label = { Text("Model (optional)") },
            placeholder = { Text(provider.defaultModel, color = PP.TextMute) },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        Gap(10.dp)
        Row(
            Modifier.bouncyClickable { uri.openUri(provider.keyUrl) }.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Get a key at ${provider.keyUrl.removePrefix("https://")}", style = MaterialTheme.typography.labelLarge, color = PP.Aqua)
            Gap(4.dp)
            Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, tint = PP.Aqua, modifier = Modifier.size(14.dp))
        }
        Gap(14.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GradientButton(
                text = if (dirty) "Save" else "Saved",
                enabled = dirty,
                onClick = {
                    vm.updateSettings { s ->
                        if (s.provider == AiProvider.GROQ) s.copy(groqKey = keyDraft, groqModel = modelDraft)
                        else s.copy(openRouterKey = keyDraft, openRouterModel = modelDraft)
                    }
                },
                modifier = Modifier.weight(1f),
            )
            GradientButton(
                text = "Test",
                enabled = !dirty && settings.hasAiKey,
                loading = connection is Async.Loading,
                onClick = vm::testConnection,
                modifier = Modifier.weight(1f),
            )
        }
        AnimatedContent(
            targetState = connection,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "connection",
        ) { state ->
            when (state) {
                is Async.Ready -> StatusLine(state.value, Icons.Rounded.CheckCircle, PP.Mint)
                is Async.Failed -> StatusLine(state.message, Icons.Rounded.ErrorOutline, PP.Rose)
                else -> Box(Modifier)
            }
        }
    }
}

@Composable
private fun StatusLine(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
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
            .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
            .padding(4.dp)
    ) {
        val w = maxWidth / options.size
        val x by animateDpAsState(w * selected.ordinal, spring(dampingRatio = 0.7f, stiffness = 450f), label = "thumb")
        Box(
            Modifier
                .offset(x = x)
                .width(w)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(PP.brandHorizontal)
        )
        Row(Modifier.fillMaxSize()) {
            options.forEach { p ->
                val color by animateColorAsState(if (p == selected) PP.Ink else PP.TextDim, label = "provColor")
                Box(
                    Modifier.weight(1f).fillMaxHeight().bouncyClickable { onSelect(p) },
                    contentAlignment = Alignment.Center,
                ) { Text(p.label, style = MaterialTheme.typography.labelLarge, color = color) }
            }
        }
    }
}

@Composable
private fun GoalCard(vm: AppViewModel, settings: AppSettings, modifier: Modifier) {
    var goalDraft by remember(settings.goalG) { mutableFloatStateOf(settings.goalG.toFloat()) }
    var weightDraft by rememberSaveable { mutableStateOf(if (settings.weightKg > 0) settings.weightKg.roundToInt().toString() else "") }

    GlassCard(modifier.fillMaxWidth()) {
        SectionLabel("Daily protein goal")
        Row(verticalAlignment = Alignment.Bottom) {
            RollingNumber(goalDraft.roundToInt(), MaterialTheme.typography.displayMedium, suffix = "g")
            Gap(10.dp)
            Text("per day", style = MaterialTheme.typography.bodyMedium, color = PP.TextDim, modifier = Modifier.padding(bottom = 10.dp))
        }
        Slider(
            value = goalDraft,
            onValueChange = { goalDraft = (it / 5).roundToInt() * 5f },
            onValueChangeFinished = { vm.updateSettings { s -> s.copy(goalG = goalDraft.roundToInt()) } },
            valueRange = 40f..250f,
            colors = SliderDefaults.colors(
                thumbColor = PP.Mint,
                activeTrackColor = PP.Mint,
                inactiveTrackColor = Color.White.copy(alpha = 0.10f),
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
        )
        Gap(8.dp)
        Text("Or calculate from your body weight", style = MaterialTheme.typography.titleMedium, color = PP.Text)
        Gap(8.dp)
        OutlinedTextField(
            value = weightDraft,
            onValueChange = { v -> weightDraft = v.filter { it.isDigit() || it == '.' }.take(5) },
            label = { Text("Weight (kg)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(18.dp),
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        Gap(10.dp)
        val weight = weightDraft.toFloatOrNull()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1.2f to "Active", 1.6f to "Build muscle", 2.0f to "Athlete").forEach { (factor, label) ->
                val target = weight?.let { ((it * factor) / 5).roundToInt() * 5 }
                Column(
                    Modifier
                        .weight(1f)
                        .bouncyClickable(enabled = target != null) {
                            val w = weight ?: return@bouncyClickable
                            val goal = ((w * factor) / 5).roundToInt().times(5).coerceIn(40, 250)
                            goalDraft = goal.toFloat()
                            vm.updateSettings { s -> s.copy(goalG = goal, weightKg = w) }
                        }
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("${factor} g/kg", style = MaterialTheme.typography.labelLarge, color = PP.Mint)
                    Text(label, style = MaterialTheme.typography.labelMedium, color = PP.TextDim)
                    if (target != null) Text("$target g", style = MaterialTheme.typography.labelMedium, color = PP.Text)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DietCard(vm: AppViewModel, settings: AppSettings, modifier: Modifier) {
    GlassCard(modifier.fillMaxWidth()) {
        SectionLabel("Diet")
        Gap(4.dp)
        Text("Your coach only suggests foods that fit.", style = MaterialTheme.typography.bodyMedium, color = PP.TextDim)
        Gap(12.dp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DietType.entries.forEach { d ->
                val selected = d == settings.diet
                val bg by animateColorAsState(if (selected) PP.Mint.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f), label = "dietBg")
                val fg by animateColorAsState(if (selected) PP.Mint else PP.TextDim, label = "dietFg")
                Text(
                    d.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = fg,
                    modifier = Modifier
                        .bouncyClickable { vm.updateSettings { it.copy(diet = d) } }
                        .clip(CircleShape)
                        .background(bg)
                        .border(1.dp, if (selected) PP.Mint.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.10f), CircleShape)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportTimeCard(vm: AppViewModel, settings: AppSettings, modifier: Modifier) {
    var picking by remember { mutableStateOf(false) }
    val timeLabel = LocalTime.of(settings.reportHour, settings.reportMinute).format(DateTimeFormatter.ofPattern("h:mm a"))

    GlassCard(modifier.fillMaxWidth()) {
        SectionLabel("End-of-day report")
        Gap(12.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(PP.Aqua.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.NotificationsActive, null, tint = PP.Aqua) }
            Gap(12.dp)
            Column(Modifier.weight(1f)) {
                Text("Daily summary notification", style = MaterialTheme.typography.titleMedium, color = PP.Text)
                Text("Protein total vs goal, every evening", style = MaterialTheme.typography.bodyMedium, color = PP.TextDim)
            }
            Switch(
                checked = settings.reportEnabled,
                onCheckedChange = { on -> vm.updateSettings(rescheduleReport = true) { it.copy(reportEnabled = on) } },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PP.Ink,
                    checkedTrackColor = PP.Mint,
                    uncheckedThumbColor = PP.TextDim,
                    uncheckedTrackColor = Color.White.copy(alpha = 0.08f),
                    uncheckedBorderColor = Color.White.copy(alpha = 0.15f),
                ),
            )
        }
        Gap(14.dp)
        Row(
            Modifier
                .fillMaxWidth()
                .bouncyClickable(enabled = settings.reportEnabled) { picking = true }
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = if (settings.reportEnabled) 0.06f else 0.02f))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Schedule, null, tint = if (settings.reportEnabled) PP.Aqua else PP.TextMute)
            Gap(10.dp)
            Text("Report time", style = MaterialTheme.typography.titleMedium, color = PP.Text, modifier = Modifier.weight(1f))
            Text(timeLabel, style = MaterialTheme.typography.titleLarge.copy(brush = Brush.horizontalGradient(listOf(PP.Aqua, PP.Mint))))
        }
    }

    if (picking) {
        val state = rememberTimePickerState(settings.reportHour, settings.reportMinute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { picking = false },
            containerColor = PP.Ink2,
            title = { Text("Report time", color = PP.Text) },
            text = {
                TimePicker(
                    state = state,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = PP.Ink3,
                        selectorColor = PP.Mint,
                        timeSelectorSelectedContainerColor = PP.Mint.copy(alpha = 0.2f),
                        timeSelectorSelectedContentColor = PP.Mint,
                        periodSelectorSelectedContainerColor = PP.Mint.copy(alpha = 0.2f),
                        periodSelectorSelectedContentColor = PP.Mint,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    picking = false
                    vm.updateSettings(rescheduleReport = true) { it.copy(reportHour = state.hour, reportMinute = state.minute) }
                }) { Text("Set", color = PP.Mint) }
            },
            dismissButton = {
                TextButton(onClick = { picking = false }) { Text("Cancel", color = PP.TextDim) }
            },
        )
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PP.Mint,
    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
    focusedContainerColor = Color.White.copy(alpha = 0.04f),
    unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
    cursorColor = PP.Mint,
    focusedTextColor = PP.Text,
    unfocusedTextColor = PP.Text,
    focusedLabelColor = PP.Mint,
    unfocusedLabelColor = PP.TextDim,
    focusedLeadingIconColor = PP.Mint,
    unfocusedLeadingIconColor = PP.TextDim,
    focusedTrailingIconColor = PP.TextDim,
    unfocusedTrailingIconColor = PP.TextMute,
)
