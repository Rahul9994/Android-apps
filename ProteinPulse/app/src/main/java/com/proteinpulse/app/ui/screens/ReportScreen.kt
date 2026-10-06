package com.proteinpulse.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.proteinpulse.app.data.AnalysisSource
import com.proteinpulse.app.data.AppSettings
import com.proteinpulse.app.data.DailyReport
import com.proteinpulse.app.data.DayTotal
import com.proteinpulse.app.data.MealEntry
import com.proteinpulse.app.ui.AppViewModel
import com.proteinpulse.app.ui.Async
import com.proteinpulse.app.ui.components.AiThinking
import com.proteinpulse.app.ui.components.Gap
import com.proteinpulse.app.ui.components.GlassCard
import com.proteinpulse.app.ui.components.GradientButton
import com.proteinpulse.app.ui.components.Pill
import com.proteinpulse.app.ui.components.RollingNumber
import com.proteinpulse.app.ui.components.SectionLabel
import com.proteinpulse.app.ui.components.bouncyClickable
import com.proteinpulse.app.ui.components.shimmer
import com.proteinpulse.app.ui.components.staggeredEntrance
import com.proteinpulse.app.ui.screenPadding
import com.proteinpulse.app.ui.theme.PP
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DayTextStyle
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun ReportScreen(vm: AppViewModel, settings: AppSettings) {
    val meals by vm.todayMeals.collectAsStateWithLifecycle()
    val week by vm.week.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val report by vm.report.collectAsStateWithLifecycle()

    val protein = meals.sumOf { it.proteinG }
    val calories = meals.sumOf { it.calories }
    val goal = settings.goalG

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = screenPadding(bottomExtra = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(Modifier.staggeredEntrance(0)) {
                Text(
                    LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM")).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = PP.TextMute,
                )
                Text("Daily report", style = MaterialTheme.typography.headlineLarge, color = PP.Text)
            }
        }

        item {
            GlassCard(Modifier.fillMaxWidth().staggeredEntrance(1)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Gauge(
                        value = if (goal > 0) (protein / goal * 100).roundToInt() else 0,
                        label = "of goal",
                        suffix = "%",
                    )
                    Gap(18.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        MiniStat("Protein", "${protein.roundToInt()} / $goal g", PP.Mint)
                        MiniStat("Calories", "${calories.roundToInt()} kcal", PP.Amber)
                        MiniStat("Meals", "${meals.size}", PP.Violet)
                        MiniStat(
                            "Avg per meal",
                            if (meals.isEmpty()) "—" else "${(protein / meals.size).roundToInt()} g",
                            PP.Aqua,
                        )
                    }
                }
            }
        }

        item {
            GlassCard(Modifier.fillMaxWidth().staggeredEntrance(2)) {
                SectionLabel("Protein timeline")
                Gap(4.dp)
                Text(
                    "Each bubble is a meal. Aim for 20–40 g every 3–5 hours.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PP.TextDim,
                )
                Gap(12.dp)
                ProteinTimeline(meals, Modifier.fillMaxWidth().height(170.dp))
            }
        }

        item {
            AnimatedContent(
                targetState = report,
                contentKey = { it::class },
                transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(150)) },
                label = "report",
                modifier = Modifier.staggeredEntrance(3),
            ) { state ->
                when (state) {
                    Async.Idle -> GlassCard(Modifier.fillMaxWidth(), tint = PP.Aqua) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Description, null, tint = PP.Aqua)
                            Gap(10.dp)
                            Text("End-of-day report", style = MaterialTheme.typography.titleLarge, color = PP.Text)
                        }
                        Gap(6.dp)
                        Text(
                            if (settings.hasAiKey) "Your AI coach reviews today's meals, scores your day and plans tomorrow."
                            else "Get a score and tips for today. Add an API key for a full AI-written review.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PP.TextDim,
                        )
                        Gap(16.dp)
                        GradientButton(
                            "Generate my report",
                            onClick = vm::generateReport,
                            icon = Icons.Rounded.AutoAwesome,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Async.Loading -> GlassCard(Modifier.fillMaxWidth()) {
                        AiThinking("Writing your report…")
                        Gap(16.dp)
                        repeat(4) { i ->
                            Box(
                                Modifier
                                    .fillMaxWidth(if (i == 3) 0.6f else 1f)
                                    .height(14.dp)
                                    .clip(CircleShape)
                                    .shimmer()
                            )
                            Gap(10.dp)
                        }
                    }
                    is Async.Failed -> GlassCard(Modifier.fillMaxWidth(), tint = PP.Rose) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Rounded.ErrorOutline, null, tint = PP.Rose)
                            Text(state.message, color = PP.Text, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    is Async.Ready -> ReportCard(state.value, state.note, onRegenerate = vm::generateReport)
                }
            }
        }

        item {
            GlassCard(Modifier.fillMaxWidth().staggeredEntrance(4)) {
                SectionLabel("Last 7 days")
                Gap(16.dp)
                WeekChart(week, goal, Modifier.fillMaxWidth().height(190.dp))
                Gap(16.dp)
                val logged = week.filter { it.meals > 0 }
                val avg = if (logged.isEmpty()) 0 else logged.map { it.proteinG }.average().roundToInt()
                val hit = week.count { it.proteinG >= goal }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    WeekStat("$avg g", "Daily avg")
                    WeekStat("$hit/7", "Goal days")
                    WeekStat("${streak(history, goal)}", "Day streak")
                }
            }
        }
    }
}

/** Consecutive days at or above goal, ending today (or yesterday, if today isn't done yet). */
private fun streak(history: List<DayTotal>, goal: Int): Int {
    if (history.isEmpty()) return 0
    val days = history.sortedByDescending { it.epochDay }
    val start = if (days.first().proteinG >= goal) 0 else 1
    return days.drop(start).takeWhile { it.proteinG >= goal }.size
}

@Composable
private fun MiniStat(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Gap(8.dp)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = PP.TextDim, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium, color = PP.Text)
    }
}

@Composable
private fun WeekStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge.merge(TextStyle(brush = PP.brandHorizontal)))
        Text(label, style = MaterialTheme.typography.labelMedium, color = PP.TextMute)
    }
}

/** A 240° arc gauge with a rolling number in the middle. */
@Composable
fun Gauge(value: Int, label: String, suffix: String = "", size: Dp = 132.dp) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(value) {
        progress.animateTo((value / 100f).coerceIn(0f, 1f), tween(1300, easing = FastOutSlowInEasing))
    }
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val inset = stroke / 2 + 2.dp.toPx()
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            drawArc(Color.White.copy(alpha = 0.07f), 150f, 240f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            if (progress.value > 0.001f) {
                drawArc(
                    Brush.sweepGradient(listOf(PP.Violet, PP.Aqua, PP.Mint, PP.Violet), center = center),
                    150f, 240f * progress.value, false, topLeft, arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                RollingNumber(value, MaterialTheme.typography.headlineLarge)
                if (suffix.isNotEmpty()) {
                    Text(suffix, style = MaterialTheme.typography.titleMedium, color = PP.TextDim, modifier = Modifier.padding(bottom = 4.dp))
                }
            }
            Text(label, style = MaterialTheme.typography.labelMedium, color = PP.TextMute)
        }
    }
}

@Composable
private fun ReportCard(report: DailyReport, note: String?, onRegenerate: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth(), tint = PP.Aqua) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Gauge(report.score, "score", size = 112.dp)
            Gap(14.dp)
            Column(Modifier.weight(1f)) {
                Text(report.headline, style = MaterialTheme.typography.titleLarge, color = PP.Text)
                Gap(6.dp)
                Pill(
                    if (report.source == AnalysisSource.AI) "AI report" else "Quick report",
                    if (report.source == AnalysisSource.AI) PP.Violet else PP.Amber,
                    icon = Icons.Rounded.AutoAwesome,
                )
            }
        }
        if (note != null) {
            Gap(8.dp)
            Text(note, style = MaterialTheme.typography.labelMedium, color = PP.Amber)
        }
        if (report.summary.isNotBlank()) {
            Gap(14.dp)
            Text(report.summary, style = MaterialTheme.typography.bodyLarge, color = PP.TextDim)
        }
        if (report.wins.isNotEmpty()) {
            Gap(16.dp)
            SectionLabel("What went well")
            report.wins.forEachIndexed { i, w -> Bullet(w, Icons.Rounded.CheckCircle, PP.Mint, i) }
        }
        if (report.improvements.isNotEmpty()) {
            Gap(14.dp)
            SectionLabel("To improve")
            report.improvements.forEachIndexed { i, w -> Bullet(w, Icons.AutoMirrored.Rounded.TrendingUp, PP.Amber, i + 3) }
        }
        if (report.tomorrow.isNotBlank()) {
            Gap(16.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(PP.Violet.copy(alpha = 0.22f), PP.Aqua.copy(alpha = 0.10f))))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Rounded.Lightbulb, null, tint = PP.Violet)
                Column {
                    Text("TOMORROW", style = MaterialTheme.typography.labelSmall, color = PP.Violet)
                    Text(report.tomorrow, style = MaterialTheme.typography.bodyMedium, color = PP.Text)
                }
            }
        }
        Gap(14.dp)
        Row(
            Modifier.align(Alignment.End).bouncyClickable(onClick = onRegenerate).padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Refresh, null, tint = PP.TextDim, modifier = Modifier.size(16.dp))
            Gap(6.dp)
            Text("Regenerate", style = MaterialTheme.typography.labelLarge, color = PP.TextDim)
        }
    }
}

@Composable
private fun Bullet(text: String, icon: ImageVector, color: Color, index: Int) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp).staggeredEntrance(index, baseDelayMs = 200),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = PP.Text)
    }
}

/** Meals plotted across the whole day; bubbles pop in with a spring, sized by protein, and stack when close. */
@Composable
private fun ProteinTimeline(meals: List<MealEntry>, modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = PP.TextMute, fontSize = 10.sp)
    val valueStyle = MaterialTheme.typography.labelMedium.copy(color = PP.Text)
    val points = remember(meals) {
        meals.sortedBy { it.timestamp }.map { m ->
            val t = Instant.ofEpochMilli(m.timestamp).atZone(ZoneId.systemDefault()).toLocalTime()
            val hours = t.hour + t.minute / 60f
            (hours / 24f).coerceIn(0f, 1f) to m.proteinG
        }
    }
    val pops = remember(meals) { points.map { Animatable(0f) } }
    LaunchedEffect(meals) {
        pops.forEachIndexed { i, a ->
            launch {
                delay(200L + i * 120L)
                a.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 220f))
            }
        }
    }

    Canvas(modifier) {
        val axisY = size.height - 22.dp.toPx()
        val pad = 14.dp.toPx()
        val w = size.width - pad * 2
        drawLine(Color.White.copy(alpha = 0.12f), Offset(pad, axisY), Offset(pad + w, axisY), 2.dp.toPx(), StrokeCap.Round)
        listOf(0 to "12am", 6 to "6am", 12 to "12pm", 18 to "6pm", 24 to "12am").forEach { (h, label) ->
            val x = pad + w * (h / 24f)
            drawLine(Color.White.copy(alpha = 0.18f), Offset(x, axisY - 4.dp.toPx()), Offset(x, axisY + 4.dp.toPx()), 1.dp.toPx())
            val layout = measurer.measure(label, labelStyle)
            drawText(layout, topLeft = Offset(x - layout.size.width / 2f, axisY + 7.dp.toPx()))
        }
        if (points.isEmpty()) {
            val layout = measurer.measure("No meals yet today", labelStyle.copy(fontSize = 13.sp))
            drawText(layout, topLeft = Offset(center.x - layout.size.width / 2f, axisY / 2 - layout.size.height / 2f))
            return@Canvas
        }
        var prevX = Float.NEGATIVE_INFINITY
        var prevTop = axisY
        points.forEachIndexed { i, (fx, grams) ->
            val pop = pops.getOrNull(i)?.value ?: 1f
            val x = pad + w * fx
            val maxR = 22.dp.toPx()
            val fullR = (sqrt(grams.toFloat().coerceAtLeast(1f) / 50f) * maxR).coerceIn(6.dp.toPx(), maxR)
            val r = fullR * pop
            // Meals close in time stack upward instead of overlapping.
            val base = if (x - prevX < maxR * 2) prevTop - 6.dp.toPx() else axisY - 14.dp.toPx()
            val cy = base - fullR
            prevX = x
            prevTop = cy - fullR - 16.dp.toPx()
            val color = PP.proteinColor(grams)
            drawLine(color.copy(alpha = 0.35f * pop.coerceAtMost(1f)), Offset(x, axisY), Offset(x, cy + r), 2.dp.toPx())
            drawCircle(color.copy(alpha = 0.18f), r * 1.35f, Offset(x, cy))
            drawCircle(Brush.radialGradient(listOf(color, color.copy(alpha = 0.6f)), Offset(x, cy), r.coerceAtLeast(1f)), r, Offset(x, cy))
            if (pop > 0.6f) {
                val layout = measurer.measure("${grams.roundToInt()}g", valueStyle)
                drawText(layout, topLeft = Offset(x - layout.size.width / 2f, (cy - r - layout.size.height - 2.dp.toPx()).coerceAtLeast(0f)))
            }
        }
    }
}

/** Seven bars that grow in sequence, with a dashed goal line. Goal days glow mint. */
@Composable
private fun WeekChart(week: List<DayTotal>, goal: Int, modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelMedium.copy(color = PP.TextMute)
    val todayStyle = MaterialTheme.typography.labelMedium.copy(color = PP.Mint)
    val valueStyle = MaterialTheme.typography.labelMedium.copy(color = PP.TextDim, fontSize = 11.sp)
    val grows = remember(week.size) { List(week.size) { Animatable(0f) } }
    LaunchedEffect(week) {
        grows.forEachIndexed { i, a ->
            launch {
                delay(150L + i * 80L)
                a.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 120f))
            }
        }
    }
    val today = LocalDate.now().toEpochDay()

    Canvas(modifier) {
        if (week.isEmpty()) return@Canvas
        val maxValue = maxOf(goal.toDouble(), week.maxOf { it.proteinG }) * 1.15
        val labelArea = 24.dp.toPx()
        val valueArea = 18.dp.toPx()
        val chartH = size.height - labelArea - valueArea
        val slot = size.width / week.size
        val barW = slot * 0.52f

        val goalY = valueArea + chartH * (1 - (goal / maxValue)).toFloat()
        drawLine(
            PP.Amber.copy(alpha = 0.7f), Offset(0f, goalY), Offset(size.width, goalY), 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())),
        )
        val goalLabel = measurer.measure("goal $goal g", valueStyle.copy(color = PP.Amber))
        drawText(goalLabel, topLeft = Offset(size.width - goalLabel.size.width, goalY - goalLabel.size.height - 2.dp.toPx()))

        week.forEachIndexed { i, day ->
            val grow = grows.getOrNull(i)?.value ?: 1f
            val fullH = (chartH * (day.proteinG / maxValue)).toFloat()
            val h = (fullH * grow).coerceAtLeast(if (day.proteinG > 0) 4.dp.toPx() else 2.dp.toPx())
            val x = slot * i + (slot - barW) / 2
            val top = valueArea + chartH - h
            val hit = day.proteinG >= goal
            drawRoundRect(Color.White.copy(alpha = 0.05f), Offset(x, valueArea), Size(barW, chartH), CornerRadius(barW / 2))
            drawRoundRect(
                brush = if (hit) Brush.verticalGradient(listOf(PP.Mint, PP.Aqua), startY = top, endY = top + h)
                else Brush.verticalGradient(listOf(PP.Violet, PP.Aqua.copy(alpha = 0.5f)), startY = top, endY = top + h),
                topLeft = Offset(x, top),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2),
            )
            if (day.proteinG > 0 && grow > 0.5f) {
                val v = measurer.measure("${day.proteinG.roundToInt()}", valueStyle)
                drawText(v, topLeft = Offset(x + barW / 2 - v.size.width / 2f, (top - v.size.height - 2.dp.toPx()).coerceAtLeast(0f)))
            }
            val name = LocalDate.ofEpochDay(day.epochDay).dayOfWeek.getDisplayName(DayTextStyle.SHORT, Locale.getDefault()).take(3)
            val label = measurer.measure(name, if (day.epochDay == today) todayStyle else labelStyle)
            drawText(label, topLeft = Offset(x + barW / 2 - label.size.width / 2f, size.height - label.size.height))
        }
    }
}
