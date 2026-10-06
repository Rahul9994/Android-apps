package com.proteinpulse.app.ai

import com.proteinpulse.app.data.AnalysisSource
import com.proteinpulse.app.data.DailyReport
import com.proteinpulse.app.data.DayTotal
import com.proteinpulse.app.data.DietType
import com.proteinpulse.app.data.MealEntry
import com.proteinpulse.app.data.Suggestion
import com.proteinpulse.app.data.SuggestionSet
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.roundToInt

/** Rule-based coaching used when there's no API key or the AI call fails. */
object OfflineCoach {

    private val catalog: Map<DietType, List<Suggestion>> = run {
        val dairy = listOf(
            Suggestion("Paneer bhurji (100 g paneer)", "🧀", 18.0, 290.0, "Fast, filling and packed with casein protein"),
            Suggestion("Greek yogurt bowl (200 g)", "🥣", 20.0, 130.0, "Light, high protein and great with fruit"),
            Suggestion("Glass of milk (300 ml)", "🥛", 10.0, 185.0, "Easy top-up any time of day"),
        )
        val plant = listOf(
            Suggestion("Soya chunk curry (50 g dry)", "🍛", 26.0, 175.0, "One of the densest plant proteins around"),
            Suggestion("Bowl of rajma or chana (250 g)", "🫘", 22.0, 340.0, "Protein plus fibre to keep you full"),
            Suggestion("Tofu stir-fry (200 g)", "🥢", 24.0, 240.0, "Complete plant protein, quick to cook"),
            Suggestion("Sprouts chaat (150 g)", "🥗", 8.0, 80.0, "Crunchy, light snack with a little protein"),
            Suggestion("Peanut butter toast (2 tbsp)", "🥜", 12.0, 330.0, "Tasty protein and healthy fats"),
        )
        val eggs = listOf(
            Suggestion("3-egg omelette", "🍳", 19.0, 230.0, "Classic, cheap and high-quality protein"),
            Suggestion("4 boiled egg whites", "🥚", 14.0, 68.0, "Almost pure protein, very low calorie"),
        )
        val meat = listOf(
            Suggestion("Grilled chicken breast (150 g)", "🍗", 46.0, 250.0, "The biggest protein hit per calorie"),
            Suggestion("Fish curry (150 g fish)", "🐟", 33.0, 260.0, "Lean protein plus omega-3s"),
            Suggestion("Tuna sandwich", "🥪", 28.0, 320.0, "Quick to make, easy to carry"),
        )
        val shake = Suggestion("Whey shake (1 scoop)", "🥤", 24.0, 120.0, "Zero-effort 24 g when you're short on time")
        val veganShake = Suggestion("Plant protein shake (1 scoop)", "🥤", 22.0, 120.0, "Quick vegan top-up")
        mapOf(
            DietType.VEGAN to plant + veganShake,
            DietType.VEG to dairy + plant + shake,
            DietType.EGG to eggs + dairy + plant + shake,
            DietType.NON_VEG to meat + eggs + dairy + plant + shake,
        )
    }

    fun suggestions(diet: DietType, goalG: Int, consumedG: Double): SuggestionSet {
        val remaining = (goalG - consumedG).coerceAtLeast(0.0)
        val hour = LocalTime.now().hour
        val options = catalog.getValue(diet)
        if (remaining <= 0.5) {
            return SuggestionSet(
                "Goal smashed! Keep it light from here",
                options.sortedBy { it.calories }.take(2),
                AnalysisSource.OFFLINE,
            )
        }
        // Late in the day favour lighter options; otherwise pick foods that best match the gap.
        val ranked = options.sortedBy { s ->
            val gapFit = abs(s.proteinG - remaining.coerceAtMost(45.0))
            val lateNightPenalty = if (hour >= 21) s.calories / 20 else 0.0
            gapFit + lateNightPenalty
        }
        return SuggestionSet(
            "${remaining.roundToInt()} g to go — here's how to close it",
            ranked.take(4),
            AnalysisSource.OFFLINE,
        )
    }

    fun report(goalG: Int, meals: List<MealEntry>, week: List<DayTotal>): DailyReport {
        val total = meals.sumOf { it.proteinG }
        val pct = if (goalG > 0) total / goalG else 0.0
        val wellSpread = meals.count { it.proteinG >= 20 }
        val score = ((pct.coerceAtMost(1.0) * 80) + (wellSpread.coerceAtMost(3) * 20 / 3.0)).roundToInt().coerceIn(0, 100)
        val daysHit = week.count { it.proteinG >= goalG }

        val wins = buildList {
            if (pct >= 1) add("Hit your ${goalG} g goal")
            if (wellSpread >= 2) add("$wellSpread meals with 20 g+ protein")
            if (daysHit >= 3) add("Goal reached on $daysHit of the last 7 days")
            if (meals.size >= 3) add("Logged ${meals.size} meals — great consistency")
            if (isEmpty() && total > 0) add("You logged your food — that's the first step")
        }
        val improvements = buildList {
            if (pct < 1) add("Add ${(goalG - total).roundToInt()} g more protein to hit your goal")
            if (meals.isNotEmpty() && wellSpread < 2) add("Aim for 20–40 g protein in each main meal")
            if (meals.size < 3) add("Spread protein across at least 3 meals")
        }
        val headline = when {
            pct >= 1 -> "Goal crushed — outstanding day!"
            pct >= 0.8 -> "So close — strong effort today"
            pct >= 0.65 -> "Good progress — finish strong"
            pct >= 0.5 -> "Halfway there — room to push"
            meals.isEmpty() -> "No meals logged today"
            else -> "A light protein day"
        }
        return DailyReport(
            score = score,
            headline = headline,
            summary = "You ate ${total.roundToInt()} g of protein against a ${goalG} g goal " +
                "(${(pct * 100).roundToInt()}%) across ${meals.size} meal${if (meals.size == 1) "" else "s"}.",
            wins = wins,
            improvements = improvements,
            tomorrow = if (pct >= 1) "Repeat today's pattern and keep each meal at 25 g+ protein."
            else "Front-load protein: aim for 30 g at breakfast so the evening isn't a catch-up.",
            source = AnalysisSource.OFFLINE,
        )
    }
}
