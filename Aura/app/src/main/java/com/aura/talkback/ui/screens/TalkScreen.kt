package com.aura.talkback.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.talkback.ai.Brain
import com.aura.talkback.data.AuraSettings
import com.aura.talkback.data.ChatMessage
import com.aura.talkback.data.Memory
import com.aura.talkback.data.Role
import com.aura.talkback.ui.MainViewModel
import com.aura.talkback.ui.components.AuraOrb
import com.aura.talkback.ui.components.Chip
import com.aura.talkback.ui.components.Gap
import com.aura.talkback.ui.components.HGap
import com.aura.talkback.ui.components.OrbMood
import com.aura.talkback.ui.components.WordReveal
import com.aura.talkback.ui.motion.LocalFrameClock
import com.aura.talkback.ui.motion.enter
import com.aura.talkback.ui.motion.pressable
import com.aura.talkback.ui.motion.rememberHaptics
import com.aura.talkback.ui.theme.A

private val suggestions = listOf(
    "I want to study DSA tonight",
    "Lock Instagram for 1 hour",
    "How's my screen time today?",
    "Roast my phone habits",
    "I have an exam on Friday",
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TalkScreen(vm: MainViewModel, s: AuraSettings) {
    val messages by vm.messages.collectAsStateWithLifecycle()
    val thinking by vm.thinking.collectAsStateWithLifecycle()
    val memories by vm.memories.collectAsStateWithLifecycle()
    val fresh by vm.freshReplyAfter.collectAsStateWithLifecycle()
    val speaking by vm.speaking.collectAsStateWithLifecycle()
    val speakingId by vm.speakingMessage.collectAsStateWithLifecycle()
    val clock = LocalFrameClock.current
    val list = rememberLazyListState()
    var showMemory by remember { mutableStateOf(false) }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val imeOpen = WindowInsets.isImeVisible

    LaunchedEffect(messages.size, thinking) {
        val count = messages.size + (if (thinking) 1 else 0)
        if (count > 0) list.animateScrollToItem(count)
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        // Header
        Row(
            Modifier.fillMaxWidth().padding(top = top + 12.dp, start = 20.dp, end = 20.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AuraOrb(
                clock, Modifier.size(52.dp),
                when { thinking -> OrbMood.THINKING; speaking -> OrbMood.SPEAKING; else -> OrbMood.CALM },
            )
            HGap(10.dp)
            Column(Modifier.weight(1f)) {
                Text("Aura", style = MaterialTheme.typography.headlineSmall, color = A.Text)
                Row(Modifier.pressable { showMemory = true }, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Psychology, null, tint = A.Iris, modifier = Modifier.size(14.dp))
                    HGap(4.dp)
                    Text(
                        if (memories.isEmpty()) "Nothing remembered yet" else "Remembers ${memories.size} thing${if (memories.size == 1) "" else "s"} · view",
                        style = MaterialTheme.typography.labelMedium, color = A.TextDim,
                    )
                }
            }
            Box(
                Modifier
                    .size(40.dp)
                    .pressable { vm.setVoice(!s.voice) }
                    .clip(CircleShape)
                    .background(if (s.voice) A.Cyan.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (s.voice) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                    if (s.voice) "Mute Aura" else "Unmute Aura",
                    tint = if (s.voice) A.Cyan else A.TextMute,
                    modifier = Modifier.size(20.dp),
                )
            }
            if (messages.isNotEmpty()) {
                HGap(10.dp)
                Icon(Icons.Rounded.DeleteSweep, "Clear chat", tint = A.TextMute, modifier = Modifier.size(22.dp).pressable { vm.stopVoice(); vm.clearChat() })
            }
        }

        LazyColumn(
            state = list,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (messages.isEmpty()) {
                item(key = "empty") { EmptyTalk(s, Modifier.enter(0)) }
            }
            items(messages, key = { it.id }) { m ->
                Bubble(
                    m,
                    animateText = m.role == Role.AURA && m.ts > fresh,
                    speakingNow = speaking && speakingId == m.id,
                    onReplay = { vm.speak(m.text, m.id) },
                    modifier = Modifier.animateItem(),
                )
            }
            if (thinking) item(key = "thinking") { ThinkingBubble(Modifier.animateItem()) }
        }

        AnimatedVisibility(messages.size < 4 && !thinking, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 10.dp),
            ) {
                items(suggestions) { Chip(it, selected = false, onClick = { vm.send(it) }) }
            }
        }

        Composer(
            enabled = !thinking,
            onSend = vm::send,
            modifier = Modifier.padding(
                start = 14.dp, end = 14.dp,
                bottom = if (imeOpen) 10.dp else navBottom + 92.dp,
            ),
        )
    }

    if (showMemory) {
        ModalBottomSheet(
            onDismissRequest = { showMemory = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = A.Night2,
            contentColor = A.Text,
        ) {
            MemorySheet(memories, onForget = vm::forget)
        }
    }
}

@Composable
private fun EmptyTalk(s: AuraSettings, modifier: Modifier) {
    Column(modifier.fillMaxWidth().padding(top = 40.dp, bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Talk to me, ${s.displayName}.", style = MaterialTheme.typography.headlineLarge, color = A.Text)
        Gap(10.dp)
        Text(
            "Tell me what you're working toward and I'll hold you to it. Ask me to lock an app, or how your day's going.",
            style = MaterialTheme.typography.bodyLarge, color = A.TextDim,
        )
        if (!s.hasAi) {
            Gap(14.dp)
            Text("No AI key yet — I'll still remember goals and lock apps, but add a key in You → AI engine to really talk.",
                style = MaterialTheme.typography.labelMedium, color = A.Amber)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Bubble(m: ChatMessage, animateText: Boolean, speakingNow: Boolean, onReplay: () -> Unit, modifier: Modifier) {
    val user = m.role == Role.USER
    Column(modifier.fillMaxWidth().enter(0, distance = 18f), horizontalAlignment = if (user) Alignment.End else Alignment.Start) {
        val shape = RoundedCornerShape(
            topStart = 24.dp, topEnd = 24.dp,
            bottomStart = if (user) 24.dp else 8.dp, bottomEnd = if (user) 8.dp else 24.dp,
        )
        Box(
            Modifier
                .widthIn(max = 310.dp)
                .clip(shape)
                .drawBehind {
                    if (user) {
                        drawRect(Brush.linearGradient(listOf(A.Iris.copy(alpha = 0.55f), A.Magenta.copy(alpha = 0.38f))))
                    } else {
                        drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.085f), Color.White.copy(alpha = 0.04f))))
                    }
                }
                .border(1.dp, Color.White.copy(alpha = if (user) 0.16f else 0.1f), shape)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            if (animateText) WordReveal(m.text, MaterialTheme.typography.bodyLarge, color = A.Text, msPerWord = 45)
            else Text(m.text, style = MaterialTheme.typography.bodyLarge, color = A.Text)
        }
        if (m.actions.isNotEmpty()) {
            Gap(6.dp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                m.actions.forEachIndexed { i, raw ->
                    val (kind, text) = raw.split('|', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
                    val (icon, color) = actionStyle(kind)
                    Row(
                        Modifier
                            .enter(i, delayMs = 400)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.12f))
                            .border(1.dp, color.copy(alpha = 0.3f), CircleShape)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
                        HGap(5.dp)
                        Text(if (kind == "mem") "Remembered: $text" else text, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 2)
                    }
                }
            }
        }
        Row(Modifier.padding(top = 4.dp, start = 6.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                Brain.timeOf(m.ts),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing),
                color = A.TextMute,
            )
            if (!user) {
                HGap(8.dp)
                AnimatedContent(
                    targetState = speakingNow,
                    transitionSpec = { (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut()) },
                    label = "speakState",
                ) { now ->
                    if (now) SoundBars()
                    else Row(
                        Modifier.pressable(scaleDown = 0.85f, onClick = onReplay).padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.VolumeUp, "Read aloud", tint = A.TextMute, modifier = Modifier.size(14.dp))
                        HGap(4.dp)
                        Text("Play", style = MaterialTheme.typography.labelMedium, color = A.TextMute)
                    }
                }
            }
        }
    }
}

/** Live equaliser shown on the reply Aura is reading aloud. */
@Composable
private fun SoundBars() {
    val t = rememberInfiniteTransition(label = "bars")
    Row(Modifier.height(16.dp).padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(A.Cyan, A.Iris, A.Magenta, A.Iris, A.Cyan).forEachIndexed { i, c ->
            val h by t.animateFloat(
                0.25f, 1f,
                infiniteRepeatable(tween(260 + i * 45, easing = LinearEasing), RepeatMode.Reverse, StartOffset(i * 70)),
                label = "bar$i",
            )
            Box(
                Modifier
                    .size(width = 3.dp, height = 16.dp)
                    .graphicsLayer { scaleY = h }
                    .clip(CircleShape)
                    .background(c)
            )
        }
        HGap(4.dp)
        Text("Speaking", style = MaterialTheme.typography.labelMedium, color = A.Cyan)
    }
}

private fun actionStyle(kind: String): Pair<ImageVector, Color> = when (kind) {
    "lock" -> Icons.Rounded.Lock to A.Magenta
    "unlock" -> Icons.Rounded.LockOpen to A.Lime
    "watch" -> Icons.Rounded.Visibility to A.Cyan
    "unwatch" -> Icons.Rounded.VisibilityOff to A.TextDim
    "mem" -> Icons.Rounded.Psychology to A.Iris
    else -> Icons.Rounded.WarningAmber to A.Amber
}

@Composable
private fun ThinkingBubble(modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "dots")
    Row(
        modifier
            .clip(RoundedCornerShape(24.dp, 24.dp, 24.dp, 8.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        listOf(A.Cyan, A.Iris, A.Magenta).forEachIndexed { i, c ->
            val y by t.animateFloat(0f, 1f, infiniteRepeatable(tween(380, easing = LinearEasing), RepeatMode.Reverse, StartOffset(i * 130)), label = "d$i")
            Box(
                Modifier
                    .size(8.dp)
                    .graphicsLayer { translationY = -y * 6.dp.toPx(); scaleX = 0.8f + 0.3f * y; scaleY = 0.8f + 0.3f * y }
                    .clip(CircleShape)
                    .background(c)
            )
        }
    }
}

@Composable
private fun Composer(enabled: Boolean, onSend: (String) -> Unit, modifier: Modifier) {
    var text by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) onSend(spoken)
        }
    }
    fun send() {
        if (text.isBlank() || !enabled) return
        haptics.confirm()
        onSend(text)
        text = ""
    }
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(A.Night2)
            .border(1.dp, Brush.linearGradient(listOf(A.Cyan.copy(alpha = 0.5f), A.Iris.copy(alpha = 0.4f), A.Magenta.copy(alpha = 0.5f))), RoundedCornerShape(30.dp))
            .padding(start = 18.dp, end = 7.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = text,
            onValueChange = { v -> if (v.contains('\n')) { text = v.replace('\n', ' ').trim(); send() } else text = v },
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = A.Text),
            cursorBrush = SolidColor(A.Cyan),
            maxLines = 4,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { send() }),
            decorationBox = { inner ->
                Box {
                    if (text.isEmpty()) Text("Talk to Aura…", style = MaterialTheme.typography.bodyLarge, color = A.TextMute)
                    inner()
                }
            },
        )
        HGap(6.dp)
        AnimatedContent(
            targetState = text.isBlank(),
            transitionSpec = { (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut()) },
            label = "sendMic",
        ) { empty ->
            Box(
                Modifier
                    .size(46.dp)
                    .pressable(enabled = enabled) {
                        if (empty) {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                .putExtra(RecognizerIntent.EXTRA_PROMPT, "Talk to Aura")
                            try { speech.launch(intent) } catch (_: ActivityNotFoundException) {
                                android.widget.Toast.makeText(context, "Voice input isn't available on this phone.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        } else send()
                    }
                    .clip(CircleShape)
                    .background(if (empty) Brush.linearGradient(listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.08f))) else A.auroraBrush),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (empty) Icons.Rounded.Mic else Icons.Rounded.ArrowUpward,
                    if (empty) "Speak" else "Send",
                    tint = if (empty) A.Text else A.Void,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
fun MemorySheet(memories: List<Memory>, onForget: (Memory) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 36.dp)) {
        Text("What I remember", style = MaterialTheme.typography.headlineSmall, color = A.Text)
        Gap(4.dp)
        Text("I bring these up when I talk back. Tap × to make me forget.", style = MaterialTheme.typography.bodyMedium, color = A.TextDim)
        Gap(16.dp)
        if (memories.isEmpty()) {
            Text("Nothing yet. Tell me your goals in Talk.", style = MaterialTheme.typography.bodyLarge, color = A.TextMute)
        }
        LazyColumn(Modifier.heightIn(max = 460.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(memories, key = { it.id }) { mem ->
                Row(
                    Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Psychology, null, tint = A.Iris, modifier = Modifier.size(18.dp))
                    HGap(10.dp)
                    Column(Modifier.weight(1f)) {
                        Text(mem.text, style = MaterialTheme.typography.bodyLarge, color = A.Text)
                        Text(Brain.relative(mem.ts), style = MaterialTheme.typography.labelMedium, color = A.TextMute)
                    }
                    Icon(Icons.Rounded.Close, "Forget", tint = A.TextMute, modifier = Modifier.size(20.dp).pressable { onForget(mem) })
                }
            }
        }
    }
}
