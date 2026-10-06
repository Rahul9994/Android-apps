package com.aura.talkback.ui.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.talkback.data.AppCatalog
import com.aura.talkback.data.AppInfo
import com.aura.talkback.data.PermissionState
import com.aura.talkback.data.Permissions
import com.aura.talkback.data.Tone
import com.aura.talkback.ui.MainViewModel
import com.aura.talkback.ui.components.AppIcon
import com.aura.talkback.ui.components.AuraOrb
import com.aura.talkback.ui.components.AuroraButton
import com.aura.talkback.ui.components.Chip
import com.aura.talkback.ui.components.Gap
import com.aura.talkback.ui.components.Glass
import com.aura.talkback.ui.components.HGap
import com.aura.talkback.ui.components.IconBadge
import com.aura.talkback.ui.components.OrbMood
import com.aura.talkback.ui.components.StatusPill
import com.aura.talkback.ui.components.WordReveal
import com.aura.talkback.ui.motion.LocalFrameClock
import com.aura.talkback.ui.motion.Springs
import com.aura.talkback.ui.motion.enter
import com.aura.talkback.ui.motion.pressable
import com.aura.talkback.ui.motion.rememberHaptics
import com.aura.talkback.ui.theme.A
import com.aura.talkback.ui.theme.Sora
import kotlinx.coroutines.delay

private const val STEPS = 6

@Composable
fun OnboardingScreen(vm: MainViewModel) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var goal by rememberSaveable { mutableStateOf("") }
    var tone by rememberSaveable { mutableStateOf(Tone.FRIEND) }
    var watched by remember { mutableStateOf<Set<String>?>(null) }
    val apps by vm.apps.collectAsStateWithLifecycle()
    val perms by vm.permissions.collectAsStateWithLifecycle()
    val haptics = rememberHaptics()

    LaunchedEffect(apps) {
        if (watched == null && apps.isNotEmpty()) {
            val installed = apps.map { it.pkg }.toSet()
            watched = AppCatalog.usualSuspects.filter { it in installed }.toSet()
        }
    }
    BackHandler(enabled = step > 0) { step-- }
    fun next() { haptics.confirm(); step = (step + 1).coerceAtMost(STEPS - 1) }

    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .padding(top = top + 12.dp, bottom = bottom + 16.dp, start = 24.dp, end = 24.dp),
    ) {
        StepBar(step)
        Gap(8.dp)
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally(spring(dampingRatio = 0.85f, stiffness = 260f)) { it / 3 * dir } + fadeIn(tween(350))) togetherWith
                    (slideOutHorizontally(tween(260)) { -it / 4 * dir } + fadeOut(tween(200)))
            },
            modifier = Modifier.weight(1f),
            label = "step",
        ) { s ->
            when (s) {
                0 -> WelcomeStep(::next)
                1 -> NameStep(name, { name = it }, ::next)
                2 -> GoalStep(goal, { goal = it }, tone, { tone = it }, ::next)
                3 -> AppsStep(apps, watched ?: emptySet(), { watched = it }, ::next)
                4 -> PermissionsStep(perms, vm, ::next)
                else -> DoneStep(name) { vm.completeOnboarding(name, goal, tone, watched ?: emptySet()) }
            }
        }
    }
}

@Composable
private fun StepBar(step: Int) {
    Row(Modifier.fillMaxWidth().height(4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(STEPS) { i ->
            val fill by animateFloatAsState(if (i <= step) 1f else 0f, spring(dampingRatio = 0.8f, stiffness = 120f), label = "seg")
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .drawBehind {
                        drawRoundRect(A.auroraH, size = Size(size.width * fill, size.height), cornerRadius = CornerRadius(size.height))
                    }
            )
        }
    }
}

@Composable
private fun StepTitle(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.headlineLarge, color = A.Text, modifier = Modifier.enter(0))
    Gap(10.dp)
    Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = A.TextDim, modifier = Modifier.enter(1))
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    val clock = LocalFrameClock.current
    var mood by remember { mutableStateOf(OrbMood.CALM) }
    LaunchedEffect(Unit) {
        while (true) { delay(2600); mood = OrbMood.SPEAKING; delay(2200); mood = OrbMood.CALM }
    }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        AuraOrb(clock, Modifier.size(300.dp).enter(0, distance = 0f), mood)
        Gap(24.dp)
        WordReveal(
            "Your phone is about to talk back.",
            style = MaterialTheme.typography.displayMedium.copy(textAlign = TextAlign.Center),
            centered = true,
            msPerWord = 120,
            startDelayMs = 500,
        )
        Gap(14.dp)
        Text(
            "Aura notices when an app is quietly eating your time — and speaks up, using what you told it you actually want.",
            style = MaterialTheme.typography.bodyLarge,
            color = A.TextDim,
            textAlign = TextAlign.Center,
            modifier = Modifier.enter(0, delayMs = 1300),
        )
        Spacer(Modifier.weight(1f))
        AuroraButton("Let's begin", onNext, Modifier.fillMaxWidth().enter(0, delayMs = 1700), icon = Icons.AutoMirrored.Rounded.ArrowForward)
    }
}

@Composable
private fun NameStep(name: String, onName: (String) -> Unit, onNext: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(top = 32.dp)) {
        StepTitle("What should I call you?", "I'll use your name when I talk to you.")
        Gap(40.dp)
        BigField(name, onName, "Your name", Modifier.enter(2), singleLine = true, onDone = { if (name.isNotBlank()) onNext() })
        Spacer(Modifier.weight(1f))
        AuroraButton("Continue", onNext, Modifier.fillMaxWidth(), enabled = name.isNotBlank(), icon = Icons.AutoMirrored.Rounded.ArrowForward)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoalStep(goal: String, onGoal: (String) -> Unit, tone: Tone, onTone: (Tone) -> Unit, onNext: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 32.dp)) {
            StepTitle("What are you working toward?", "When you're scrolling, I'll remind you of this — in your own words.")
            Gap(28.dp)
            BigField(goal, onGoal, "e.g. Study DSA every night", Modifier.enter(2), singleLine = false)
            Gap(14.dp)
            FlowRow(Modifier.enter(3), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Study DSA every night", "Sleep before midnight", "Finish my project", "Get fit", "Read more").forEach {
                    Chip(it, selected = goal == it, onClick = { onGoal(it) })
                }
            }
            Gap(34.dp)
            Text("HOW SHOULD I TALK TO YOU?", style = MaterialTheme.typography.labelSmall, color = A.TextMute, modifier = Modifier.enter(4))
            Gap(12.dp)
            ToneCards(tone, onTone, Modifier.enter(5))
        }
        Gap(16.dp)
        AuroraButton("Continue", onNext, Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Rounded.ArrowForward)
    }
}

@Composable
fun ToneCards(tone: Tone, onTone: (Tone) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Tone.entries.forEach { t ->
            val sel = t == tone
            val glow by animateFloatAsState(if (sel) 1f else 0f, Springs.gentle(), label = "toneGlow")
            Column(
                Modifier
                    .weight(1f)
                    .heightIn(min = 112.dp)
                    .pressable { onTone(t) }
                    .clip(RoundedCornerShape(22.dp))
                    .drawBehind {
                        drawRect(Color.White.copy(alpha = 0.04f + 0.04f * glow))
                        drawRect(Brush.linearGradient(listOf(A.Iris.copy(alpha = 0.32f * glow), A.Magenta.copy(alpha = 0.12f * glow))))
                    }
                    .border(1.dp, if (sel) A.Iris.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.10f), RoundedCornerShape(22.dp))
                    .padding(14.dp),
            ) {
                Text(
                    when (t) { Tone.FRIEND -> "🫶"; Tone.COACH -> "⚡"; Tone.SAVAGE -> "🔥" },
                    fontSize = 22.sp,
                )
                Gap(8.dp)
                Text(t.label, style = MaterialTheme.typography.titleMedium, color = A.Text)
                Text(t.blurb, style = MaterialTheme.typography.labelMedium, color = A.TextDim)
            }
        }
    }
}

@Composable
private fun BigField(
    value: String,
    onValue: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean,
    onDone: () -> Unit = {},
) {
    val clock = LocalFrameClock.current
    BasicTextField(
        value = value,
        onValueChange = onValue,
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 3,
        textStyle = TextStyle(fontFamily = Sora, fontSize = if (singleLine) 34.sp else 24.sp, lineHeight = if (singleLine) 40.sp else 32.sp, color = A.Text),
        cursorBrush = SolidColor(A.Cyan),
        keyboardOptions = KeyboardOptions(
            capitalization = if (singleLine) KeyboardCapitalization.Words else KeyboardCapitalization.Sentences,
            imeAction = if (singleLine) ImeAction.Done else ImeAction.Default,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Column {
                Box {
                    if (value.isEmpty()) {
                        Text(hint, style = TextStyle(fontFamily = Sora, fontSize = if (singleLine) 34.sp else 24.sp, color = A.TextMute))
                    }
                    inner()
                }
                Gap(12.dp)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .drawBehind {
                            drawRect(Color.White.copy(alpha = 0.1f))
                            val x = ((clock.floatValue * 0.25f) % 1f) * size.width
                            drawRect(Brush.horizontalGradient(listOf(Color.Transparent, A.Cyan, A.Iris, A.Magenta, Color.Transparent), x - size.width * 0.5f, x + size.width * 0.5f))
                        }
                )
            }
        },
    )
}

@Composable
private fun AppsStep(apps: List<AppInfo>, watched: Set<String>, onWatched: (Set<String>) -> Unit, onNext: () -> Unit) {
    val haptics = rememberHaptics()
    val sorted = remember(apps) {
        val suspects = AppCatalog.usualSuspects
        apps.sortedWith(compareBy({ it.pkg !in suspects }, { it.label.lowercase() }))
    }
    Column(Modifier.fillMaxSize().padding(top = 32.dp)) {
        StepTitle("Which apps steal your time?", "I'll keep an eye on these. You can change them any time.")
        Gap(20.dp)
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(sorted, key = { it.pkg }) { app ->
                val sel = app.pkg in watched
                val scale by animateFloatAsState(if (sel) 1f else 0.9f, Springs.bouncy(), label = "appScale")
                Column(
                    Modifier
                        .pressable(haptic = false) { haptics.tick(); onWatched(if (sel) watched - app.pkg else watched + app.pkg) }
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        Box(
                            Modifier
                                .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (sel) 1f else 0.55f }
                                .clip(RoundedCornerShape(20.dp))
                                .border(2.dp, if (sel) A.auroraBrush else SolidColor(Color.Transparent), RoundedCornerShape(20.dp))
                                .padding(5.dp)
                        ) { AppIcon(app.pkg, 50.dp) }
                        androidx.compose.animation.AnimatedVisibility(sel, enter = scaleIn(Springs.bouncy()) + fadeIn(), exit = scaleOut() + fadeOut()) {
                            Box(
                                Modifier.size(20.dp).clip(CircleShape).background(A.auroraBrush),
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Rounded.Check, null, tint = A.Void, modifier = Modifier.size(14.dp)) }
                        }
                    }
                    Gap(6.dp)
                    Text(app.label, style = MaterialTheme.typography.labelMedium, color = if (sel) A.Text else A.TextMute, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Gap(12.dp)
        AuroraButton(
            if (watched.isEmpty()) "Pick at least one" else "Watch ${watched.size} app${if (watched.size == 1) "" else "s"}",
            onNext, Modifier.fillMaxWidth(), enabled = watched.isNotEmpty(), icon = Icons.Rounded.Visibility,
        )
    }
}

@Composable
private fun PermissionsStep(perms: PermissionState, vm: MainViewModel, onNext: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 32.dp)) {
            StepTitle("Let me see what's happening", "Everything stays on this phone. I only see app names — never your screen or messages.")
            Gap(24.dp)
            PermissionList(perms, vm)
        }
        Gap(14.dp)
        AuroraButton(
            if (perms.coreReady) "Continue" else "Allow the first two to continue",
            onNext, Modifier.fillMaxWidth(), enabled = perms.coreReady, icon = Icons.AutoMirrored.Rounded.ArrowForward,
        )
        if (!perms.coreReady) {
            Text(
                "Skip for now",
                style = MaterialTheme.typography.labelLarge,
                color = A.TextMute,
                modifier = Modifier.align(Alignment.CenterHorizontally).pressable(onClick = onNext).padding(12.dp),
            )
        }
    }
}

/** Reused in Settings: each permission with live status and a button to grant it. */
@Composable
fun PermissionList(perms: PermissionState, vm: MainViewModel) {
    val context = LocalContext.current
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.onResume() }
    fun open(intent: android.content.Intent) {
        try { context.startActivity(intent) } catch (_: ActivityNotFoundException) { context.startActivity(Permissions.appDetailsIntent(context)) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PermissionRow(Icons.Rounded.QueryStats, A.Cyan, "Usage access", "Measures your screen time per app", perms.usage, 0) {
            open(Permissions.usageIntent(context))
        }
        PermissionRow(Icons.Rounded.AccessibilityNew, A.Iris, "Accessibility", "Notices when you open an app, so I can talk back over it", perms.accessibility, 1) {
            open(Permissions.accessibilityIntent())
        }
        PermissionRow(Icons.Rounded.NotificationsActive, A.Magenta, "Notification access", "Counts how often apps ping you (optional)", perms.notificationAccess, 2) {
            open(Permissions.notificationAccessIntent())
        }
        if (Build.VERSION.SDK_INT >= 33) {
            PermissionRow(Icons.Rounded.NotificationsActive, A.Lime, "Notifications", "Lets me reach you (optional)", perms.postNotifications, 3) {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (!perms.accessibility) {
            Text(
                "Android may say “Restricted setting”. If so: tap here → ⋮ menu → Allow restricted settings, then try again.",
                style = MaterialTheme.typography.labelMedium,
                color = A.TextMute,
                modifier = Modifier.pressable { open(Permissions.appDetailsIntent(context)) }.padding(4.dp),
            )
        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    color: Color,
    title: String,
    body: String,
    granted: Boolean,
    index: Int,
    onClick: () -> Unit,
) {
    Glass(Modifier.fillMaxWidth().enter(index + 2).pressable(enabled = !granted, onClick = onClick), padding = PaddingValues(16.dp), glow = if (granted) null else color) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, color, 44.dp)
            HGap(14.dp)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = A.Text)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = A.TextDim)
            }
            HGap(8.dp)
            AnimatedContent(
                targetState = granted,
                transitionSpec = { (scaleIn(Springs.bouncy()) + fadeIn()) togetherWith (scaleOut() + fadeOut()) },
                label = "perm",
            ) { ok ->
                if (ok) StatusPill("On", A.Lime, icon = Icons.Rounded.Check)
                else StatusPill("Allow", color)
            }
        }
    }
}

@Composable
private fun DoneStep(name: String, onFinish: () -> Unit) {
    val clock = LocalFrameClock.current
    var mood by remember { mutableStateOf(OrbMood.ALERT) }
    LaunchedEffect(Unit) { delay(900); mood = OrbMood.SPEAKING; delay(2600); mood = OrbMood.CALM }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        AuraOrb(clock, Modifier.size(280.dp).enter(0, distance = 0f), mood)
        Gap(24.dp)
        WordReveal(
            "I'm listening, ${name.ifBlank { "friend" }}.",
            style = MaterialTheme.typography.displayMedium.copy(textAlign = TextAlign.Center),
            centered = true,
            msPerWord = 140,
            startDelayMs = 300,
        )
        Gap(14.dp)
        Text(
            "Go live your life. I'll speak up when it matters — and you can talk to me any time.",
            style = MaterialTheme.typography.bodyLarge,
            color = A.TextDim,
            textAlign = TextAlign.Center,
            modifier = Modifier.enter(0, delayMs = 1000),
        )
        Spacer(Modifier.weight(1f))
        AuroraButton("Enter Aura", onFinish, Modifier.fillMaxWidth().enter(0, delayMs = 1400), icon = Icons.AutoMirrored.Rounded.ArrowForward)
    }
}
