package com.proteinpulse.app.data

import org.json.JSONArray
import org.json.JSONObject

data class FoodItem(
    val name: String,
    val quantity: String,
    val proteinG: Double,
    val calories: Double,
)

enum class AnalysisSource { AI, OFFLINE }

data class MealEntry(
    val id: Long,
    val timestamp: Long,
    val epochDay: Long,
    val description: String,
    val items: List<FoodItem>,
    val proteinG: Double,
    val calories: Double,
    val source: AnalysisSource,
    val tip: String?,
)

data class MealAnalysis(
    val items: List<FoodItem>,
    val tip: String?,
    val source: AnalysisSource,
) {
    val proteinG: Double get() = items.sumOf { it.proteinG }
    val calories: Double get() = items.sumOf { it.calories }
}

data class DayTotal(
    val epochDay: Long,
    val proteinG: Double,
    val calories: Double,
    val meals: Int,
)

data class Suggestion(
    val food: String,
    val emoji: String,
    val proteinG: Double,
    val calories: Double,
    val why: String,
)

data class SuggestionSet(
    val headline: String,
    val suggestions: List<Suggestion>,
    val source: AnalysisSource,
)

data class DailyReport(
    val score: Int,
    val headline: String,
    val summary: String,
    val wins: List<String>,
    val improvements: List<String>,
    val tomorrow: String,
    val source: AnalysisSource,
)

fun List<FoodItem>.toJson(): String = JSONArray().also { arr ->
    forEach { item ->
        arr.put(
            JSONObject()
                .put("name", item.name)
                .put("quantity", item.quantity)
                .put("protein_g", item.proteinG)
                .put("calories", item.calories)
        )
    }
}.toString()

fun parseFoodItems(json: String?): List<FoodItem> {
    if (json.isNullOrBlank()) return emptyList()
    return runCatching {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i -> arr.getJSONObject(i).toFoodItem() }
    }.getOrDefault(emptyList())
}

fun JSONObject.toFoodItem() = FoodItem(
    name = optString("name").ifBlank { "Food" },
    quantity = optString("quantity"),
    proteinG = optDouble("protein_g", 0.0).takeIf { !it.isNaN() }?.coerceAtLeast(0.0) ?: 0.0,
    calories = optDouble("calories", 0.0).takeIf { !it.isNaN() }?.coerceAtLeast(0.0) ?: 0.0,
)
