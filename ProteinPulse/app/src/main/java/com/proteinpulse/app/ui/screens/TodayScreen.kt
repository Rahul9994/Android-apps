package com.proteinpulse.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.proteinpulse.app.data.AnalysisSource
import com.proteinpulse.app.data.AppSettings
import com.proteinpulse.app.data.MealEntry
import com.proteinpulse.app.ui.AppViewModel
import com.proteinpulse.app.ui.LogState
import com.proteinpulse.app.ui.Tab
import com.proteinpulse.app.ui.components.AiThinking
import com.proteinpulse.app.ui.components.Gap
import com.proteinpulse.app.ui.components.GlassCard
import com.proteinpulse.app.ui.components.Pill
import com.proteinpulse.app.ui.components.ProteinRing
import com.proteinpulse.app.ui.components.RollingNumber
import com.proteinpulse.app.ui.components.SectionLabel
import com.proteinpulse.app.ui.components.bouncyClickable
import com.proteinpulse.app.ui.components.staggeredEntrance
import com.proteinpulse.app.ui.screenPadding
import com.proteinpulse.app.ui.theme.PP
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun TodayScreen(vm: AppViewModel, settings: AppSettings, snackbar: SnackbarHostState) {
    val meals by vm.todayMeals.collectAsStateWithLifecycle()
    val recent by vm.recent.collectAsStateWithLifecycle()
    val log by vm.log.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val protein = meals.sumOf { it.proteinG }
    val calories = meals.sumOf { it.calories }

    // Bring the analysis card into view when a meal is being logged.
    val listState = rememberLazyListState()
    val logIndex = if (settings.hasAiKey) 2 else 3
    LaunchedEffect(log::class) {
        if (log is LogState.Analyzing || log is LogState.Logged || log is LogState.Failed) {
            listState.animateScrollToItem(logIndex, scrollOffset = -120)
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        state = listState,
        contentPadding = screenPadding(bottomExtra = 190.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "header") {
            TodayHeader(settings, Modifier.staggeredEntrance(0)) { vm.selectTab(Tab.SETTINGS) }
        }
        item(key = "hero") {
            GlassCard(Modifier.fillMaxWidth().staggeredEntrance(1), padding = PaddingValues(vertical = 24.dp, horizontal = 16.dp)) {
                ProteinRing(protein, settings.goalG, Modifier.align(Alignment.CenterHorizontally))
                Gap(20.dp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Remaining", (settings.goalG - protein).coerceAtLeast(0.0).roundToInt(), "g", Icons.Rounded.Bolt, PP.Aqua, Modifier.weight(1f))
                    StatTile("Meals", meals.size, null, Icons.Rounded.Restaurant, PP.Violet, Modifier.weight(1f))
                    StatTile("Calories", calories.roundToInt(), "kcal", Icons.Rounded.LocalFireDepartment, PP.Amber, Modifier.weight(1f))
                }
            }
        }
        if (!settings.hasAiKey) {
            item(key = "keyBanner") {
                KeyBanner(Modifier.staggeredEntrance(2)) { vm.selectTab(Tab.SETTINGS) }
            }
        }
        item(key = "log") {
            LogResultCard(log, onDismiss = vm::dismissLog)
        }
        if (recent.isNotEmpty()) {
            item(key = "recentLabel") { SectionLabel("Quick re-log", Modifier.staggeredEntrance(3)) }
            item(key = "recent") {
                val rowState = rememberLazyListState()
                LaunchedEffect(recent.firstOrNull()?.id) { rowState.animateScrollToItem(0) }
                LazyRow(
                    Modifier.staggeredEntrance(3),
                    state = rowState,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(recent, key = { it.id }) { entry -> RecentChip(entry) { vm.relog(entry) } }
                }
            }
        }
        item(key = "mealsLabel") {
            SectionLabel("Today's meals", Modifier.staggeredEntrance(4)) {
                Text("${meals.size}", style = MaterialTheme.typography.labelMedium, color = PP.TextDim)
            }
        }
        if (meals.isEmpty()) {
            item(key = "empty") { EmptyMeals(Modifier.staggeredEntrance(5)) }
        }
        items(meals, key = { it.id }) { meal ->
            SwipeableMeal(
                meal = meal,
                modifier = Modifier.animateItem(),
                onDelete = {
                    vm.delete(meal)
                    scope.launch {
                        val result = snackbar.showSnackbar("Meal removed", actionLabel = "Undo", duration = SnackbarDuration.Short)
                        if (result == SnackbarResult.ActionPerformed) vm.undoDelete()
                    }
                },
            )
        }
    }
}

@Composable
private fun TodayHeader(settings: AppSettings, modifier: Modifier, onStatusClick: () -> Unit) {
    val hour = LocalTime.now().hour
    val greeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Late night fuel"
    }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM")).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = PP.TextMute,
            )
            Text(greeting, style = MaterialTheme.typography.headlineLarge, color = PP.Text)
        }
        Box(Modifier.bouncyClickable(onClick = onStatusClick)) {
            if (settings.hasAiKey) Pill("${settings.provider.label} AI", PP.Mint, icon = Icons.Rounded.AutoAwesome)
            else Pill("Offline", PP.Amber, icon = Icons.Rounded.CloudOff)
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: Int,
    unit: String?,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.16f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Box(
            Modifier.size(30.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = color, modifier = Modifier.size(17.dp)) }
        Gap(10.dp)
        Row(verticalAlignment = Alignment.Bottom) {
            RollingNumber(value, MaterialTheme.typography.titleLarge)
            if (unit != null) {
                Text(" $unit", style = MaterialTheme.typography.labelMedium, color = PP.TextDim, modifier = Modifier.padding(bottom = 3.dp))
            }
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = PP.TextMute)
    }
}

@Composable
private fun KeyBanner(modifier: Modifier, onClick: () -> Unit) {
    GlassCard(modifier.fillMaxWidth().bouncyClickable(onClick = onClick), tint = PP.Amber, padding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(PP.Amber.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.AutoAwesome, null, tint = PP.Amber) }
            Column(Modifier.weight(1f)) {
                Text("Turn on AI analysis", style = MaterialTheme.typography.titleMedium, color = PP.Text)
                Text(
                    "Using offline estimates for now. Add a Groq or OpenRouter key for accurate breakdowns.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PP.TextDim,
                )
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = PP.Amber)
        }
    }
}

@Composable
private fun LogResultCard(log: LogState, onDismiss: () -> Unit) {
    // Keep showing the last real state while the card animates out.
    val lastShown = remember { arrayOf<LogState>(LogState.Idle) }
    if (log !is LogState.Idle) lastShown[0] = log
    val shown = lastShown[0]

    AnimatedVisibility(
        visible = log !is LogState.Idle,
        enter = expandVertically(spring(dampingRatio = 0.8f, stiffness = 300f)) + fadeIn() + scaleIn(initialScale = 0.92f),
        exit = shrinkVertically() + fadeOut(),
    ) {
        GlassCard(
            Modifier.fillMaxWidth(),
            tint = when (shown) {
                is LogState.Failed -> PP.Rose
                is LogState.Logged -> PP.Mint
                else -> PP.Violet
            },
        ) {
            AnimatedContent(
                targetState = shown,
                contentKey = { it::class },
                transitionSpec = { (fadeIn(tween(300)) + slideInVertically { it / 4 }) togetherWith fadeOut(tween(150)) },
                label = "logState",
            ) { state ->
                when (state) {
                    is LogState.Analyzing -> Column {
                        AiThinking("Analyzing your plate…")
                        Gap(8.dp)
                        Text("“${state.text}”", style = MaterialTheme.typography.bodyMedium, color = PP.TextDim, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    is LogState.Logged -> LoggedContent(state, onDismiss)
                    is LogState.Failed -> Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Rounded.ErrorOutline, null, tint = PP.Rose)
                        Text(state.message, style = MaterialTheme.typography.bodyMedium, color = PP.Text, modifier = Modifier.weight(1f))
                        Icon(Icons.Rounded.Close, "Dismiss", tint = PP.TextDim, modifier = Modifier.bouncyClickable(onClick = onDismiss))
                    }
                    LogState.Idle -> Box(Modifier)
                }
            }
        }
    }
}

@Composable
private fun LoggedContent(state: LogState.Logged, onDismiss: () -> Unit) {
    val a = state.analysis
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "+${a.proteinG.roundToInt()} g",
                style = MaterialTheme.typography.displayMedium.merge(TextStyle(brush = PP.brand)),
            )
            Gap(10.dp)
            Column(Modifier.weight(1f)) {
                Text("protein logged", style = MaterialTheme.typography.titleMedium, color = PP.Text)
                Text("${a.calories.roundToInt()} kcal", style = MaterialTheme.typography.bodyMedium, color = PP.TextDim)
            }
            Icon(Icons.Rounded.Close, "Dismiss", tint = PP.TextDim, modifier = Modifier.bouncyClickable(onClick = onDismiss))
        }
        Gap(12.dp)
        a.items.forEachIndexed { i, item ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 5.dp).staggeredEntrance(i, baseDelayMs = 150),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(PP.proteinColor(item.proteinG)))
                Gap(10.dp)
                Column(Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium, color = PP.Text)
                    if (item.quantity.isNotBlank()) {
                        Text(item.quantity, style = MaterialTheme.typography.bodyMedium, color = PP.TextMute)
                    }
                }
                Text("${item.proteinG.oneDecimal()} g", style = MaterialTheme.typography.titleMedium, color = PP.proteinColor(item.proteinG))
            }
        }
        if (!a.tip.isNullOrBlank()) {
            Gap(10.dp)
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.05f)).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Rounded.Lightbulb, null, tint = PP.Amber, modifier = Modifier.size(18.dp))
                Text(a.tip, style = MaterialTheme.typography.bodyMedium, color = PP.TextDim)
            }
        }
        if (state.note != null) {
            Gap(8.dp)
            Text(state.note, style = MaterialTheme.typography.labelMedium, color = if (a.source == AnalysisSource.AI) PP.TextMute else PP.Amber)
        }
    }
}

@Composable
private fun RecentChip(entry: MealEntry, onClick: () -> Unit) {
    Row(
        Modifier
            .bouncyClickable(onClick = onClick)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
            .padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Rounded.Replay, null, tint = PP.Mint, modifier = Modifier.size(16.dp))
        Text(
            entry.description.take(26) + if (entry.description.length > 26) "…" else "",
            style = MaterialTheme.typography.labelLarge,
            color = PP.Text,
        )
        Text("${entry.proteinG.roundToInt()} g", style = MaterialTheme.typography.labelMedium, color = PP.Mint)
    }
}

@Composable
private fun EmptyMeals(modifier: Modifier) {
    val bob = rememberInfiniteTransition(label = "bob")
    val y by bob.animateFloat(
        0f, 1f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bobY",
    )
    GlassCard(modifier.fillMaxWidth(), padding = PaddingValues(28.dp)) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .graphicsLayer { translationY = -y * 8.dp.toPx() }
                .size(64.dp)
                .clip(CircleShape)
                .background(PP.brand),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Restaurant, null, tint = PP.Ink, modifier = Modifier.size(30.dp)) }
        Gap(16.dp)
        Text(
            "Nothing logged yet",
            style = MaterialTheme.typography.titleLarge,
            color = PP.Text,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Gap(4.dp)
        Text(
            "Type what you ate below, like “3 eggs and a glass of milk”.",
            style = MaterialTheme.typography.bodyMedium,
            color = PP.TextDim,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableMeal(meal: MealEntry, modifier: Modifier, onDelete: () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete(); true
            } else false
        },
    )
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val active = state.targetValue == SwipeToDismissBoxValue.EndToStart
            val scale by animateFloatAsState(if (active) 1.25f else 0.85f, spring(dampingRatio = 0.4f), label = "trash")
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(PP.Rose.copy(alpha = if (active) 0.35f else 0.12f))
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Rounded.Delete, "Delete", tint = PP.Rose, modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale })
            }
        },
    ) {
        MealRow(meal)
    }
}

@Composable
private fun MealRow(meal: MealEntry) {
    var expanded by rememberSaveable(meal.id) { mutableStateOf(false) }
    val arrow by animateFloatAsState(if (expanded) 180f else 0f, spring(dampingRatio = 0.6f), label = "arrow")
    val color = PP.proteinColor(meal.proteinG)
    val time = remember(meal.timestamp) { SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(meal.timestamp)) }

    GlassCard(
        Modifier.fillMaxWidth().bouncyClickable { expanded = !expanded },
        shape = RoundedCornerShape(24.dp),
        padding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(color.copy(alpha = 0.35f), color.copy(alpha = 0.08f))))
                    .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${meal.proteinG.roundToInt()}", style = MaterialTheme.typography.titleLarge, color = PP.Text)
                    Text("g", style = MaterialTheme.typography.labelSmall, color = color)
                }
            }
            Gap(14.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    meal.description.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    color = PP.Text,
                    maxLines = if (expanded) 4 else 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("$time · ${meal.calories.roundToInt()} kcal", style = MaterialTheme.typography.bodyMedium, color = PP.TextMute)
                    if (meal.source == AnalysisSource.AI) {
                        Icon(Icons.Rounded.AutoAwesome, "AI analysed", tint = PP.Violet, modifier = Modifier.size(13.dp))
                    }
                }
            }
            Icon(
                Icons.Rounded.KeyboardArrowDown, null, tint = PP.TextMute,
                modifier = Modifier.graphicsLayer { rotationZ = arrow },
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(spring(dampingRatio = 0.8f, stiffness = 400f)) + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(Modifier.padding(top = 12.dp)) {
                meal.items.forEach { item ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            item.name + if (item.quantity.isNotBlank()) "  ·  ${item.quantity}" else "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PP.TextDim,
                            modifier = Modifier.weight(1f),
                        )
                        Text("${item.proteinG.oneDecimal()} g", style = MaterialTheme.typography.labelLarge, color = PP.proteinColor(item.proteinG))
                    }
                }
                if (!meal.tip.isNullOrBlank()) {
                    Gap(6.dp)
                    Text(meal.tip, style = MaterialTheme.typography.bodyMedium, color = PP.TextMute)
                }
                Gap(6.dp)
                Text("Swipe left to delete", style = MaterialTheme.typography.labelSmall, color = PP.TextMute)
            }
        }
    }
}

private val examples = listOf(
    "3 eggs and 2 rotis",
    "200 g chicken breast with rice",
    "a bowl of dal and 1 cup curd",
    "whey shake with 300 ml milk",
    "paneer tikka, 150 g",
    "masala dosa and sambar",
)

/** The "What did you eat?" bar pinned above the nav bar on the Today tab. */
@Composable
fun MealComposer(vm: AppViewModel) {
    val log by vm.log.collectAsStateWithLifecycle()
    var text by rememberSaveable { mutableStateOf("") }
    var lastSubmitted by rememberSaveable { mutableStateOf("") }
    val focus = LocalFocusManager.current
    val analyzing = log is LogState.Analyzing

    // Give the text back if analysis failed, so it can be fixed and re-sent.
    LaunchedEffect(log) {
        if (log is LogState.Failed && text.isEmpty()) text = lastSubmitted
    }

    fun submit() {
        if (text.isBlank() || analyzing) return
        lastSubmitted = text
        vm.logMeal(text)
        text = ""
        focus.clearFocus()
    }

    var exampleIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(3200)
            exampleIndex = (exampleIndex + 1) % examples.size
        }
    }

    val shape = RoundedCornerShape(30.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 62.dp)
            .clip(shape)
            .background(PP.Ink2)
            .border(
                1.dp,
                Brush.linearGradient(listOf(PP.Mint.copy(alpha = 0.55f), PP.Violet.copy(alpha = 0.35f))),
                shape,
            )
            .padding(start = 18.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Restaurant, null, tint = PP.Mint, modifier = Modifier.size(20.dp))
        Gap(12.dp)
        BasicTextField(
            value = text,
            onValueChange = { value ->
                // Enter sends instead of starting a new line.
                if (value.contains('\n')) {
                    text = value.replace("\n", " ").trim()
                    submit()
                } else {
                    text = value
                }
            },
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = PP.Text),
            cursorBrush = SolidColor(PP.Mint),
            maxLines = 3,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { submit() }),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (text.isEmpty()) {
                        AnimatedContent(
                            targetState = exampleIndex,
                            transitionSpec = {
                                (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                            },
                            label = "placeholder",
                        ) { i ->
                            Text(
                                "e.g. ${examples[i]}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = PP.TextMute,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    inner()
                }
            },
        )
        Gap(8.dp)
        SendButton(analyzing = analyzing, enabled = text.isNotBlank(), onClick = ::submit)
    }
}

@Composable
private fun SendButton(analyzing: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val spin = rememberInfiniteTransition(label = "spin")
    val angle by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "angle")
    val scale by animateFloatAsState(if (enabled || analyzing) 1f else 0.86f, spring(dampingRatio = 0.5f), label = "sendScale")

    Box(
        Modifier
            .size(46.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .bouncyClickable(enabled = enabled && !analyzing, onClick = onClick)
            .clip(CircleShape)
            .background(if (enabled || analyzing) PP.brand else SolidColor(Color.White.copy(alpha = 0.08f))),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(targetState = analyzing, label = "sendState") { busy ->
            if (busy) {
                Canvas(Modifier.size(24.dp)) {
                    rotate(angle) {
                        drawArc(
                            color = PP.Ink,
                            startAngle = 0f,
                            sweepAngle = 270f,
                            useCenter = false,
                            style = Stroke(3.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }
                }
            } else {
                Icon(
                    Icons.AutoMirrored.Rounded.Send, "Log meal",
                    tint = if (enabled) PP.Ink else PP.TextMute,
                    modifier = Modifier.size(20.dp).graphicsLayer { translationX = 1.dp.toPx() },
                )
            }
        }
    }
}

internal fun Double.oneDecimal(): String {
    val r = (this * 10).roundToInt() / 10.0
    return if (r % 1.0 == 0.0) r.roundToInt().toString() else r.toString()
}
