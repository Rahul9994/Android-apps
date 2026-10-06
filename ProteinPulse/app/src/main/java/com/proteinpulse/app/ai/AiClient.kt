package com.proteinpulse.app.ai

import com.proteinpulse.app.data.AiProvider
import com.proteinpulse.app.data.AnalysisSource
import com.proteinpulse.app.data.AppSettings
import com.proteinpulse.app.data.DailyReport
import com.proteinpulse.app.data.DayTotal
import com.proteinpulse.app.data.MealAnalysis
import com.proteinpulse.app.data.MealEntry
import com.proteinpulse.app.data.Suggestion
import com.proteinpulse.app.data.SuggestionSet
import com.proteinpulse.app.data.toFoodItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class AiException(message: String) : Exception(message)

/** Talks to any OpenAI-compatible chat endpoint (Groq and OpenRouter both are). */
class AiClient {
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeMeal(settings: AppSettings, text: String): MealAnalysis {
        val system = """
            You are a precise sports nutritionist. Estimate the protein and calories in what the user ate.
            Split the meal into individual foods. When a portion isn't given, assume a typical home portion
            (Indian portions when the dish is Indian). Be realistic, not generous.
            Reply with ONLY a JSON object, no prose:
            {"items":[{"name":"string","quantity":"string, e.g. 2 pieces (~100 g)","protein_g":number,"calories":number}],
             "tip":"one short sentence (max 18 words) about the protein quality of this meal or a better swap"}
            If the text is not food, reply {"items":[],"tip":"explain briefly"}.
        """.trimIndent()

        val json = chat(settings, system, "I ate: $text", temperature = 0.2)
        val items = json.optJSONArray("items").orEmpty().map { it.toFoodItem() }
        if (items.isEmpty()) {
            throw AiException(json.optString("tip").ifBlank { "That didn't look like food. Try something like \"2 eggs and toast\"." })
        }
        return MealAnalysis(items, json.optString("tip").ifBlank { null }, AnalysisSource.AI)
    }

    suspend fun suggestions(
        settings: AppSettings,
        consumedG: Double,
        todaysMeals: List<MealEntry>,
    ): SuggestionSet {
        val remaining = (settings.goalG - consumedG).coerceAtLeast(0.0)
        val system = """
            You are an upbeat, practical protein coach. Suggest foods the user can eat next to reach today's protein goal.
            Respect the diet strictly. Prefer easy, affordable, everyday foods (Indian options welcome).
            Fit the time of day (no heavy dinner suggestions at breakfast time). 3 or 4 suggestions.
            Reply with ONLY JSON:
            {"headline":"max 12 words, motivating and specific",
             "suggestions":[{"food":"food with portion","emoji":"one food emoji","protein_g":number,"calories":number,"why":"max 14 words"}]}
        """.trimIndent()
        val user = buildString {
            appendLine("Local time: ${timeNow()}")
            appendLine("Diet: ${settings.diet.promptLabel}")
            appendLine("Daily protein goal: ${settings.goalG} g")
            appendLine("Eaten so far today: ${consumedG.roundToInt()} g. Still needed: ${remaining.roundToInt()} g.")
            appendLine("Today's meals: ${mealsSummary(todaysMeals)}")
        }
        val json = chat(settings, system, user, temperature = 0.7)
        val list = json.optJSONArray("suggestions").orEmpty().map { o ->
            Suggestion(
                food = o.optString("food"),
                emoji = o.optString("emoji").ifBlank { "🍽️" },
                proteinG = o.optDouble("protein_g", 0.0).let { if (it.isNaN()) 0.0 else it },
                calories = o.optDouble("calories", 0.0).let { if (it.isNaN()) 0.0 else it },
                why = o.optString("why"),
            )
        }.filter { it.food.isNotBlank() }
        if (list.isEmpty()) throw AiException("The AI didn't return any suggestions. Try again.")
        return SuggestionSet(json.optString("headline").ifBlank { "Here's how to close the gap" }, list, AnalysisSource.AI)
    }

    suspend fun dailyReport(
        settings: AppSettings,
        todaysMeals: List<MealEntry>,
        week: List<DayTotal>,
    ): DailyReport {
        val total = todaysMeals.sumOf { it.proteinG }
        val system = """
            You are a supportive but honest nutrition coach writing an end-of-day protein report.
            Judge total vs goal, how evenly protein was spread across meals (20-40 g per meal is ideal),
            protein quality, and the weekly trend. Keep language simple and encouraging.
            Reply with ONLY JSON:
            {"score":integer 0-100,"headline":"max 10 words","summary":"2-3 sentences",
             "wins":["max 3 short points"],"improvements":["max 3 short, concrete points"],
             "tomorrow":"one concrete plan for tomorrow, max 30 words"}
        """.trimIndent()
        val user = buildString {
            appendLine("Diet: ${settings.diet.promptLabel}")
            appendLine("Goal: ${settings.goalG} g protein. Eaten today: ${total.roundToInt()} g across ${todaysMeals.size} meals.")
            appendLine("Meals today (time — food — protein):")
            todaysMeals.sortedBy { it.timestamp }.forEach {
                appendLine("${timeOf(it.timestamp)} — ${it.description} — ${it.proteinG.roundToInt()} g")
            }
            appendLine("Last 7 days protein (oldest first): ${week.joinToString { it.proteinG.roundToInt().toString() }}")
        }
        val json = chat(settings, system, user, temperature = 0.5)
        return DailyReport(
            score = json.optInt("score", 0).coerceIn(0, 100),
            headline = json.optString("headline").ifBlank { "Your day in protein" },
            summary = json.optString("summary"),
            wins = json.optJSONArray("wins").strings(),
            improvements = json.optJSONArray("improvements").strings(),
            tomorrow = json.optString("tomorrow"),
            source = AnalysisSource.AI,
        )
    }

    /** Cheap round trip to confirm the key and model work. */
    suspend fun testConnection(settings: AppSettings): String {
        val json = chat(settings, "Reply with ONLY JSON: {\"ok\":true}", "ping", temperature = 0.0)
        return if (json.optBoolean("ok", true)) "Connected to ${settings.provider.label} · ${modelFor(settings)}"
        else "Unexpected reply from the model"
    }

    /** Replacement models picked after the configured one turned out to be retired, per provider. */
    private val resolvedModels = mutableMapOf<AiProvider, String>()

    private fun modelFor(settings: AppSettings): String = resolvedModels[settings.provider] ?: settings.activeModel

    private class HttpResult(val code: Int, val text: String) {
        val apiMessage: String? =
            runCatching { JSONObject(text).getJSONObject("error").optString("message") }.getOrNull()

        /** 404, or Groq's 400 "model_decommissioned" / "does not exist" style errors. */
        val isModelMissing: Boolean
            get() {
                val msg = text.lowercase()
                return code == 404 || (code == 400 && "model" in msg &&
                    ("decommission" in msg || "does not exist" in msg || "not found" in msg || "not a valid model" in msg))
            }
    }

    private suspend fun chat(settings: AppSettings, system: String, user: String, temperature: Double): JSONObject =
        withContext(Dispatchers.IO) {
            if (!settings.hasAiKey) throw AiException("Add your ${settings.provider.label} API key in Settings.")

            var model = modelFor(settings)
            var result = postChat(settings, model, system, user, temperature)

            if (result.isModelMissing) {
                val replacement = pickAvailableModel(settings, exclude = model)
                    ?: throw AiException(
                        "${settings.provider.label} no longer offers \"$model\". Enter a current model name in Settings."
                    )
                resolvedModels[settings.provider] = replacement
                model = replacement
                result = postChat(settings, model, system, user, temperature)
            }

            if (result.code !in 200..299) {
                throw AiException(
                    when {
                        result.code == 401 || result.code == 403 ->
                            "Your ${settings.provider.label} API key was rejected. Check it in Settings."
                        result.isModelMissing -> "Model \"$model\" isn't available on ${settings.provider.label}."
                        result.code == 429 -> "Rate limit reached. Wait a moment and try again."
                        else -> result.apiMessage?.takeIf { it.isNotBlank() } ?: "AI request failed (HTTP ${result.code})."
                    }
                )
            }

            val content = runCatching {
                JSONObject(result.text).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
            }.getOrElse { throw AiException("The AI sent back an empty reply. Try again.") }

            extractJson(content) ?: throw AiException("The AI reply wasn't in the expected format. Try again.")
        }

    private fun postChat(settings: AppSettings, model: String, system: String, user: String, temperature: Double): HttpResult {
        val body = JSONObject()
            .put("model", model)
            .put("temperature", temperature)
            .put("messages", JSONArray()
                .put(JSONObject().put("role", "system").put("content", system))
                .put(JSONObject().put("role", "user").put("content", user)))
        if (settings.provider == AiProvider.GROQ) {
            body.put("response_format", JSONObject().put("type", "json_object"))
            // gpt-oss models think before answering; keep it short so logging a meal stays snappy.
            if (model.startsWith("openai/gpt-oss")) body.put("reasoning_effort", "low")
        }
        val request = Request.Builder()
            .url(settings.provider.chatEndpoint)
            .header("Authorization", "Bearer ${settings.activeKey}")
            .apply { if (settings.provider == AiProvider.OPENROUTER) header("X-Title", "Protein Pulse") }
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        return send(settings, request)
    }

    /** Asks the provider which models exist and returns the best match from our preference list. */
    private fun pickAvailableModel(settings: AppSettings, exclude: String): String? {
        val request = Request.Builder()
            .url(settings.provider.modelsEndpoint)
            .header("Authorization", "Bearer ${settings.activeKey}")
            .get()
            .build()
        val result = send(settings, request)
        if (result.code !in 200..299) return null
        val available = runCatching {
            val data = JSONObject(result.text).getJSONArray("data")
            (0 until data.length()).map { data.getJSONObject(it).optString("id") }.toSet()
        }.getOrDefault(emptySet())
        return settings.provider.preferredModels.firstOrNull { it in available && it != exclude }
    }

    private fun send(settings: AppSettings, request: Request): HttpResult = try {
        http.newCall(request).execute().use { HttpResult(it.code, it.body?.string().orEmpty()) }
    } catch (e: IOException) {
        throw AiException("Couldn't reach ${settings.provider.label}. Check your internet connection.")
    }

    private fun extractJson(content: String): JSONObject? {
        val start = content.indexOf('{')
        val end = content.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return runCatching { JSONObject(content.substring(start, end + 1)) }.getOrNull()
    }

    private fun mealsSummary(meals: List<MealEntry>) =
        if (meals.isEmpty()) "nothing yet"
        else meals.sortedBy { it.timestamp }
            .joinToString("; ") { "${timeOf(it.timestamp)} ${it.description} (${it.proteinG.roundToInt()} g)" }

    private fun timeNow() = SimpleDateFormat("EEEE h:mm a", Locale.getDefault()).format(Date())
    private fun timeOf(ts: Long) = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ts))
}

private fun JSONArray?.orEmpty(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

private fun JSONArray?.strings(): List<String> =
    if (this == null) emptyList() else (0 until length()).map { optString(it) }.filter { it.isNotBlank() }
