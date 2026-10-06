package com.aura.talkback.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

enum class AiProvider(
    val label: String,
    val baseUrl: String,
    /** Auto mode picks the first of these that the provider still lists. Strongest first. */
    val preferredModels: List<String>,
    val keyUrl: String,
) {
    GROQ(
        "Groq",
        "https://api.groq.com/openai/v1",
        listOf(
            "openai/gpt-oss-120b",
            "moonshotai/kimi-k2-instruct-0905",
            "qwen/qwen3.8-27b",
            "openai/gpt-oss-20b",
            "llama-3.3-70b-versatile",
        ),
        "https://console.groq.com/keys",
    ),
    OPENROUTER(
        "OpenRouter",
        "https://openrouter.ai/api/v1",
        listOf(
            "openai/gpt-oss-120b",
            "google/gemini-2.5-flash",
            "deepseek/deepseek-chat-v3.1",
            "meta-llama/llama-3.3-70b-instruct",
            "openai/gpt-oss-20b:free",
            "meta-llama/llama-3.3-70b-instruct:free",
        ),
        "https://openrouter.ai/keys",
    );

    val chatEndpoint get() = "$baseUrl/chat/completions"
    val modelsEndpoint get() = "$baseUrl/models"
}

enum class Tone(val label: String, val blurb: String, val prompt: String) {
    FRIEND("Friend", "Warm, gentle, on your side", "a warm, caring best friend — gentle but honest"),
    COACH("Coach", "Direct and motivating", "a sharp, motivating coach — direct, no fluff, believes in them"),
    SAVAGE("Savage", "Brutally honest, funny", "a brutally honest, witty friend who roasts them lovingly — sarcastic but never cruel"),
}

data class AuraSettings(
    val onboarded: Boolean = false,
    val userName: String = "",
    val tone: Tone = Tone.FRIEND,
    val watched: Set<String> = emptySet(),
    val monitoring: Boolean = true,
    val nudgeAfterMin: Int = 20,
    val nightNudgeAfterMin: Int = 5,
    val nightStartHour: Int = 23,
    val nightEndHour: Int = 5,
    val defaultLockMin: Int = 30,
    val voice: Boolean = true,
    val provider: AiProvider = AiProvider.GROQ,
    val groqKey: String = "",
    val openRouterKey: String = "",
    /** Empty = Auto (best available). */
    val groqModel: String = "",
    val openRouterModel: String = "",
    /** Locked packages → unlock time (epoch ms). */
    val locks: Map<String, Long> = emptyMap(),
) {
    val activeKey get() = if (provider == AiProvider.GROQ) groqKey else openRouterKey
    val chosenModel get() = if (provider == AiProvider.GROQ) groqModel else openRouterModel
    val hasAi get() = activeKey.isNotBlank()
    val displayName get() = userName.ifBlank { "friend" }

    fun activeLocks(now: Long = System.currentTimeMillis()) = locks.filterValues { it > now }

    fun isNight(hour: Int): Boolean =
        if (nightStartHour <= nightEndHour) hour in nightStartHour until nightEndHour
        else hour >= nightStartHour || hour < nightEndHour
}

private val Context.dataStore by preferencesDataStore(name = "aura_settings")

class SettingsStore(private val context: Context) {
    private object K {
        val onboarded = booleanPreferencesKey("onboarded")
        val name = stringPreferencesKey("name")
        val tone = stringPreferencesKey("tone")
        val watched = stringSetPreferencesKey("watched")
        val monitoring = booleanPreferencesKey("monitoring")
        val nudgeAfter = intPreferencesKey("nudge_after")
        val nightNudgeAfter = intPreferencesKey("night_nudge_after")
        val nightStart = intPreferencesKey("night_start")
        val nightEnd = intPreferencesKey("night_end")
        val defaultLock = intPreferencesKey("default_lock")
        val voice = booleanPreferencesKey("voice")
        val provider = stringPreferencesKey("provider")
        val groqKey = stringPreferencesKey("groq_key")
        val openRouterKey = stringPreferencesKey("openrouter_key")
        val groqModel = stringPreferencesKey("groq_model")
        val openRouterModel = stringPreferencesKey("openrouter_model")
        val locks = stringPreferencesKey("locks")
    }

    val flow: Flow<AuraSettings> = context.dataStore.data.map { it.toSettings() }

    suspend fun current(): AuraSettings = flow.first()

    suspend fun update(transform: (AuraSettings) -> AuraSettings) {
        context.dataStore.edit { p ->
            val s = transform(p.toSettings())
            p[K.onboarded] = s.onboarded
            p[K.name] = s.userName.trim()
            p[K.tone] = s.tone.name
            p[K.watched] = s.watched
            p[K.monitoring] = s.monitoring
            p[K.nudgeAfter] = s.nudgeAfterMin
            p[K.nightNudgeAfter] = s.nightNudgeAfterMin
            p[K.nightStart] = s.nightStartHour
            p[K.nightEnd] = s.nightEndHour
            p[K.defaultLock] = s.defaultLockMin
            p[K.voice] = s.voice
            p[K.provider] = s.provider.name
            p[K.groqKey] = s.groqKey.trim()
            p[K.openRouterKey] = s.openRouterKey.trim()
            p[K.groqModel] = s.groqModel.trim()
            p[K.openRouterModel] = s.openRouterModel.trim()
            val now = System.currentTimeMillis()
            p[K.locks] = JSONObject().apply { s.locks.filterValues { it > now }.forEach { (k, v) -> put(k, v) } }.toString()
        }
    }

    suspend fun lock(pkg: String, minutes: Int) =
        update { it.copy(locks = it.locks + (pkg to System.currentTimeMillis() + minutes * 60_000L)) }

    suspend fun unlock(pkg: String) = update { it.copy(locks = it.locks - pkg) }

    private fun Preferences.toSettings(): AuraSettings {
        val d = AuraSettings()
        val locks = runCatching {
            val o = JSONObject(this[K.locks] ?: "{}")
            o.keys().asSequence().associateWith { o.getLong(it) }
        }.getOrDefault(emptyMap())
        return AuraSettings(
            onboarded = this[K.onboarded] ?: d.onboarded,
            userName = this[K.name] ?: d.userName,
            tone = this[K.tone]?.let { runCatching { Tone.valueOf(it) }.getOrNull() } ?: d.tone,
            watched = this[K.watched] ?: d.watched,
            monitoring = this[K.monitoring] ?: d.monitoring,
            nudgeAfterMin = this[K.nudgeAfter] ?: d.nudgeAfterMin,
            nightNudgeAfterMin = this[K.nightNudgeAfter] ?: d.nightNudgeAfterMin,
            nightStartHour = this[K.nightStart] ?: d.nightStartHour,
            nightEndHour = this[K.nightEnd] ?: d.nightEndHour,
            defaultLockMin = this[K.defaultLock] ?: d.defaultLockMin,
            voice = this[K.voice] ?: d.voice,
            provider = this[K.provider]?.let { runCatching { AiProvider.valueOf(it) }.getOrNull() } ?: d.provider,
            groqKey = this[K.groqKey] ?: d.groqKey,
            openRouterKey = this[K.openRouterKey] ?: d.openRouterKey,
            groqModel = this[K.groqModel] ?: d.groqModel,
            openRouterModel = this[K.openRouterModel] ?: d.openRouterModel,
            locks = locks,
        )
    }
}
