package com.proteinpulse.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.proteinpulse.app.data.AnalysisSource
import com.proteinpulse.app.data.AppSettings
import com.proteinpulse.app.data.Suggestion
import com.proteinpulse.app.data.SuggestionSet
import com.proteinpulse.app.ui.AppViewModel
import com.proteinpulse.app.ui.Async
import com.proteinpulse.app.ui.components.AiThinking
import com.proteinpulse.app.ui.components.Gap
import com.proteinpulse.app.ui.components.GlassCard
import com.proteinpulse.app.ui.components.GradientButton
import com.proteinpulse.app.ui.components.Pill
import com.proteinpulse.app.ui.components.ProgressBar
import com.proteinpulse.app.ui.components.RollingNumber
import com.proteinpulse.app.ui.components.SectionLabel
import com.proteinpulse.app.ui.components.bouncyClickable
import com.proteinpulse.app.ui.components.shimmer
import com.proteinpulse.app.ui.components.staggeredEntrance
import com.proteinpulse.app.ui.screenPadding
import com.proteinpulse.app.ui.theme.PP
import java.time.LocalTime
import kotlin.math.roundToInt

@Composable
fun CoachScreen(vm: AppViewModel, settings: AppSettings) {
    val meals by vm.todayMeals.collectAsStateWithLifecycle()
    val suggestions by vm.suggestions.collectAsStateWithLifecycle()
    val consumed = meals.sumOf { it.proteinG }

    LaunchedEffect(Unit) {
        if (suggestions is Async.Idle) vm.loadSuggestions()
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = screenPadding(bottomExtra = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(Modifier.staggeredEntrance(0)) {
                Text("YOUR AI COACH", style = MaterialTheme.typography.labelSmall, color = PP.TextMute)
                Text("What to eat next", style = MaterialTheme.typography.headlineLarge, color = PP.Text)
            }
        }
        item { RemainingCard(consumed, settings.goalG, Modifier.staggeredEntrance(1)) }
        item {
            GradientButton(
                text = if (suggestions is Async.Ready) "Refresh suggestions" else "Get smart suggestions",
                icon = if (suggestions is Async.Ready) Icons.Rounded.Refresh else Icons.Rounded.AutoAwesome,
                loading = suggestions is Async.Loading,
                onClick = vm::loadSuggestions,
                modifier = Modifier.fillMaxWidth().staggeredEntrance(2),
            )
        }
        item {
            AnimatedContent(
                targetState = suggestions,
                contentKey = { it::class },
                transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(150)) },
                label = "suggestions",
            ) { state ->
                when (state) {
                    Async.Idle -> Box(Modifier)
                    Async.Loading -> SuggestionSkeleton()
                    is Async.Failed -> GlassCard(Modifier.fillMaxWidth(), tint = PP.Rose) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Rounded.ErrorOutline, null, tint = PP.Rose)
                            Text(state.message, color = PP.Text, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    is Async.Ready -> SuggestionList(state.value, state.note, onLog = vm::logSuggestion)
                }
            }
        }
        item { SectionLabel("Protein principles", Modifier.staggeredEntrance(4)) }
        item {
            LazyRow(Modifier.staggeredEntrance(5), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(principles) { i, p -> PrincipleCard(p.first, p.second, principleColors[i % principleColors.size]) }
            }
        }
    }
}

@Composable
private fun RemainingCard(consumed: Double, goal: Int, modifier: Modifier) {
    val remaining = (goal - consumed).coerceAtLeast(0.0)
    val progress = if (goal > 0) (consumed / goal).toFloat() else 0f
    val hour = LocalTime.now().hour
    // Expected share of the goal by now, assuming eating from 7 am to 10 pm.
    val expected = ((hour - 7) / 15f).coerceIn(0f, 1f)
    val pace = when {
        progress >= 1f -> "Goal reached — anything more is a bonus." to PP.Amber
        progress + 0.15f < expected -> "Behind pace. Make your next meal protein-first." to PP.Rose
        else -> "On pace. Keep 20–40 g in each meal." to PP.Mint
    }

    GlassCard(modifier.fillMaxWidth(), tint = PP.Violet) {
        Text("STILL NEEDED TODAY", style = MaterialTheme.typography.labelSmall, color = PP.TextMute)
        Row(verticalAlignment = Alignment.Bottom) {
            RollingNumber(remaining.roundToInt(), MaterialTheme.typography.displayMedium, suffix = "g")
            Gap(12.dp)
            Text(
                "${consumed.roundToInt()} of $goal g eaten",
                style = MaterialTheme.typography.bodyMedium,
                color = PP.TextDim,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }
        Gap(12.dp)
        ProgressBar(progress, Modifier.fillMaxWidth(), height = 12.dp)
        Gap(12.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(pace.second))
            Gap(8.dp)
            Text(pace.first, style = MaterialTheme.typography.bodyMedium, color = PP.TextDim)
        }
    }
}

@Composable
private fun SuggestionSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AiThinking("Your coach is thinking…", Modifier.padding(vertical = 6.dp))
        repeat(3) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .shimmer()
            )
        }
    }
}

@Composable
private fun SuggestionList(set: SuggestionSet, note: String?, onLog: (Suggestion) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                set.headline,
                style = MaterialTheme.typography.headlineSmall.merge(TextStyle(brush = PP.brandHorizontal)),
                modifier = Modifier.weight(1f).staggeredEntrance(0),
            )
            if (set.source == AnalysisSource.AI) Pill("AI", PP.Violet, icon = Icons.Rounded.AutoAwesome)
        }
        if (note != null) Text(note, style = MaterialTheme.typography.labelMedium, color = PP.Amber)
        set.suggestions.forEachIndexed { i, s ->
            SuggestionCard(s, Modifier.staggeredEntrance(i + 1, baseDelayMs = 80), onLog)
        }
    }
}

@Composable
private fun SuggestionCard(s: Suggestion, modifier: Modifier, onLog: (Suggestion) -> Unit) {
    var logged by remember(s) { mutableStateOf(false) }
    GlassCard(modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(PP.Violet.copy(alpha = 0.35f), PP.Aqua.copy(alpha = 0.15f)))),
                contentAlignment = Alignment.Center,
            ) { Text(s.emoji, fontSize = 26.sp) }
            Gap(14.dp)
            Column(Modifier.weight(1f)) {
                Text(s.food, style = MaterialTheme.typography.titleMedium, color = PP.Text)
                Text(s.why, style = MaterialTheme.typography.bodyMedium, color = PP.TextDim)
            }
        }
        Gap(12.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill("+${s.proteinG.roundToInt()} g protein", PP.Mint)
            Gap(8.dp)
            Text("${s.calories.roundToInt()} kcal", style = MaterialTheme.typography.labelMedium, color = PP.TextMute)
            Box(Modifier.weight(1f))
            AnimatedContent(targetState = logged, label = "logBtn") { done ->
                Row(
                    Modifier
                        .bouncyClickable(enabled = !done) { logged = true; onLog(s) }
                        .clip(CircleShape)
                        .background(if (done) PP.Mint.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f))
                        .border(1.dp, if (done) PP.Mint.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.14f), CircleShape)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (done) {
                        Icon(Icons.Rounded.Check, null, tint = PP.Mint, modifier = Modifier.size(16.dp))
                        Gap(4.dp)
                    }
                    Text(if (done) "Logged" else "I ate this", style = MaterialTheme.typography.labelLarge, color = if (done) PP.Mint else PP.Text)
                }
            }
        }
    }
}

private val principles = listOf(
    "Spread it out" to "20–40 g per meal beats one huge protein dinner for muscle building.",
    "Protein first" to "Eat the protein part of your plate first — it keeps you full longer.",
    "Pair plant proteins" to "Dal + rice or hummus + roti together give a complete amino-acid profile.",
    "Don't skip breakfast" to "A 25–30 g breakfast makes the rest of the day far easier.",
    "Easy wins" to "Curd, eggs, paneer, soya and a whey scoop are the cheapest grams.",
)
private val principleColors = listOf(PP.Mint, PP.Aqua, PP.Violet, PP.Amber, PP.Rose)

@Composable
private fun PrincipleCard(title: String, body: String, color: Color) {
    GlassCard(Modifier.width(220.dp).height(150.dp), tint = color, padding = PaddingValues(16.dp)) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Gap(10.dp)
        Text(title, style = MaterialTheme.typography.titleMedium, color = PP.Text)
        Gap(4.dp)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = PP.TextDim)
    }
}
