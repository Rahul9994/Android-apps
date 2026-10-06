package com.proteinpulse.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class AiProvider(
    val label: String,
    val baseUrl: String,
    /** Tried in order when the chosen model is retired; the first that the provider still lists wins. */
    val preferredModels: List<String>,
    val keyUrl: String,
) {
    GROQ(
        "Groq",
        "https://api.groq.com/openai/v1",
        listOf("openai/gpt-oss-120b", "openai/gpt-oss-20b", "qwen/qwen3.8-27b"),
        "https://console.groq.com/keys",
    ),
    OPENROUTER(
        "OpenRouter",
        "https://openrouter.ai/api/v1",
        listOf("openai/gpt-oss-120b", "meta-llama/llama-3.3-70b-instruct", "openai/gpt-oss-20b"),
        "https://openrouter.ai/keys",
    );

    val defaultModel: String get() = preferredModels.first()
    val chatEndpoint: String get() = "$baseUrl/chat/completions"
    val modelsEndpoint: String get() = "$baseUrl/models"
}

enum class DietType(val label: String, val promptLabel: String) {
    VEG("Vegetarian", "vegetarian (dairy ok, no eggs, no meat or fish)"),
    EGG("Eggetarian", "vegetarian plus eggs (no meat or fish)"),
    NON_VEG("Non-veg", "non-vegetarian (eats meat, fish, eggs and dairy)"),
    VEGAN("Vegan", "vegan (no animal products)"),
}

data class AppSettings(
    val provider: AiProvider = AiProvider.GROQ,
    val groqKey: String = "",
    val openRouterKey: String = "",
    val groqModel: String = "",
    val openRouterModel: String = "",
    val goalG: Int = 120,
    val weightKg: Float = 0f,
    val diet: DietType = DietType.NON_VEG,
    val reportEnabled: Boolean = true,
    val reportHour: Int = 21,
    val reportMinute: Int = 0,
) {
    val activeKey: String get() = if (provider == AiProvider.GROQ) groqKey else openRouterKey
    val activeModel: String
        get() = (if (provider == AiProvider.GROQ) groqModel else openRouterModel)
            .ifBlank { provider.defaultModel }
    val hasAiKey: Boolean get() = activeKey.isNotBlank()
}

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {
    private object Keys {
        val provider = stringPreferencesKey("provider")
        val groqKey = stringPreferencesKey("groq_key")
        val openRouterKey = stringPreferencesKey("openrouter_key")
        val groqModel = stringPreferencesKey("groq_model")
        val openRouterModel = stringPreferencesKey("openrouter_model")
        val goal = intPreferencesKey("goal_g")
        val weight = floatPreferencesKey("weight_kg")
        val diet = stringPreferencesKey("diet")
        val reportEnabled = booleanPreferencesKey("report_enabled")
        val reportHour = intPreferencesKey("report_hour")
        val reportMinute = intPreferencesKey("report_minute")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { it.toSettings() }

    suspend fun current(): AppSettings = settings.first()

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val s = transform(prefs.toSettings())
            prefs[Keys.provider] = s.provider.name
            prefs[Keys.groqKey] = s.groqKey.trim()
            prefs[Keys.openRouterKey] = s.openRouterKey.trim()
            prefs[Keys.groqModel] = s.groqModel.trim()
            prefs[Keys.openRouterModel] = s.openRouterModel.trim()
            prefs[Keys.goal] = s.goalG
            prefs[Keys.weight] = s.weightKg
            prefs[Keys.diet] = s.diet.name
            prefs[Keys.reportEnabled] = s.reportEnabled
            prefs[Keys.reportHour] = s.reportHour
            prefs[Keys.reportMinute] = s.reportMinute
        }
    }

    private fun Preferences.toSettings(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            provider = this[Keys.provider]?.let { runCatching { AiProvider.valueOf(it) }.getOrNull() } ?: d.provider,
            groqKey = this[Keys.groqKey] ?: d.groqKey,
            openRouterKey = this[Keys.openRouterKey] ?: d.openRouterKey,
            groqModel = this[Keys.groqModel] ?: d.groqModel,
            openRouterModel = this[Keys.openRouterModel] ?: d.openRouterModel,
            goalG = this[Keys.goal] ?: d.goalG,
            weightKg = this[Keys.weight] ?: d.weightKg,
            diet = this[Keys.diet]?.let { runCatching { DietType.valueOf(it) }.getOrNull() } ?: d.diet,
            reportEnabled = this[Keys.reportEnabled] ?: d.reportEnabled,
            reportHour = this[Keys.reportHour] ?: d.reportHour,
            reportMinute = this[Keys.reportMinute] ?: d.reportMinute,
        )
    }
}
