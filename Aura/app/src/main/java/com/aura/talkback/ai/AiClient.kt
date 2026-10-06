package com.aura.talkback.ai

import com.aura.talkback.data.AiProvider
import com.aura.talkback.data.AuraSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class AiException(message: String) : Exception(message)

data class ChatTurn(val role: String, val content: String)

/**
 * OpenAI-compatible client for Groq and OpenRouter.
 * "Auto" model = the strongest model from [AiProvider.preferredModels] that the provider still lists;
 * if a chosen model is retired (or an OpenRouter account has no credits) it falls back on its own.
 */
class AiClient {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    private val autoCache = mutableMapOf<AiProvider, String>()
    private val modelsCache = mutableMapOf<AiProvider, List<String>>()

    /** The model a request will use right now. */
    suspend fun currentModel(s: AuraSettings): String =
        s.chosenModel.ifBlank { autoModel(s) }

    /** Chat-capable model ids offered by the provider, recommended ones first. */
    suspend fun listModels(s: AuraSettings, refresh: Boolean = false): List<String> = withContext(Dispatchers.IO) {
        if (!s.hasAi) throw AiException("Add your ${s.provider.label} key first.")
        if (!refresh) modelsCache[s.provider]?.let { return@withContext it }
        val res = send(s, Request.Builder().url(s.provider.modelsEndpoint).header("Authorization", "Bearer ${s.activeKey}").get().build())
        if (res.code == 401 || res.code == 403) throw AiException("Your ${s.provider.label} key was rejected.")
        if (res.code !in 200..299) throw AiException(res.apiMessage ?: "Couldn't load models (HTTP ${res.code}).")
        val ids = runCatching {
            val data = JSONObject(res.text).getJSONArray("data")
            (0 until data.length()).map { data.getJSONObject(it).optString("id") }
        }.getOrDefault(emptyList()).filter { isChatModel(it) }
        val preferred = s.provider.preferredModels
        val sorted = preferred.filter { it in ids } + ids.filter { it !in preferred }.sorted()
        modelsCache[s.provider] = sorted
        sorted
    }

    suspend fun test(s: AuraSettings): String {
        val json = chatJson(s, listOf(ChatTurn("system", "Reply with ONLY this JSON: {\"ok\":true}"), ChatTurn("user", "ping")), 0.0)
        return if (json.optBoolean("ok", true)) "Connected · ${currentModel(s)}" else "Unexpected reply from the model"
    }

    suspend fun chatJson(s: AuraSettings, turns: List<ChatTurn>, temperature: Double): JSONObject = withContext(Dispatchers.IO) {
        if (!s.hasAi) throw AiException("Add your ${s.provider.label} API key in Settings.")
        var model = currentModel(s)
        var res = post(s, model, turns, temperature)

        if (res.isModelMissing) {
            autoCache.remove(s.provider)
            modelsCache.remove(s.provider)
            model = autoModel(s, exclude = setOf(model))
            res = post(s, model, turns, temperature)
        }
        if (res.code == 402 && s.provider == AiProvider.OPENROUTER) {
            // No credits: fall back to a free model.
            val free = runCatching { listModels(s, refresh = true) }.getOrDefault(emptyList())
                .firstOrNull { it.endsWith(":free") }
            if (free != null) {
                autoCache[s.provider] = free
                model = free
                res = post(s, model, turns, temperature)
            }
        }
        if (res.code !in 200..299) {
            throw AiException(
                when {
                    res.code == 401 || res.code == 403 -> "Your ${s.provider.label} key was rejected. Check it in Settings."
                    res.code == 402 -> "Your OpenRouter account is out of credits."
                    res.code == 429 -> "Rate limit reached. Try again in a moment."
                    res.isModelMissing -> "Model \"$model\" isn't available. Pick another in Settings."
                    else -> res.apiMessage?.takeIf { it.isNotBlank() } ?: "AI request failed (HTTP ${res.code})."
                }
            )
        }
        val content = runCatching {
            JSONObject(res.text).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
        }.getOrElse { throw AiException("The AI sent an empty reply.") }
        extractJson(content) ?: throw AiException("The AI reply wasn't in the expected format.")
    }

    private suspend fun autoModel(s: AuraSettings, exclude: Set<String> = emptySet()): String {
        autoCache[s.provider]?.takeIf { it !in exclude }?.let { return it }
        val available = runCatching { listModels(s, refresh = exclude.isNotEmpty()) }.getOrNull()
        val preferred = s.provider.preferredModels.filter { it !in exclude }
        val pick = when {
            available.isNullOrEmpty() -> preferred.firstOrNull() ?: s.provider.preferredModels.first()
            else -> preferred.firstOrNull { it in available } ?: available.first { it !in exclude }
        }
        if (!available.isNullOrEmpty()) autoCache[s.provider] = pick
        return pick
    }

    private fun post(s: AuraSettings, model: String, turns: List<ChatTurn>, temperature: Double): HttpResult {
        val messages = JSONArray().apply { turns.forEach { put(JSONObject().put("role", it.role).put("content", it.content)) } }
        val body = JSONObject().put("model", model).put("temperature", temperature).put("messages", messages)
        if (s.provider == AiProvider.GROQ) {
            body.put("response_format", JSONObject().put("type", "json_object"))
            if (model.startsWith("openai/gpt-oss")) body.put("reasoning_effort", "low")
            if (model.contains("qwen3")) body.put("reasoning_format", "hidden")
        }
        val request = Request.Builder()
            .url(s.provider.chatEndpoint)
            .header("Authorization", "Bearer ${s.activeKey}")
            .apply { if (s.provider == AiProvider.OPENROUTER) header("X-Title", "Aura") }
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        return send(s, request)
    }

    private fun send(s: AuraSettings, request: Request): HttpResult = try {
        http.newCall(request).execute().use { HttpResult(it.code, it.body?.string().orEmpty()) }
    } catch (e: IOException) {
        throw AiException("Couldn't reach ${s.provider.label}. Check your internet.")
    }

    private class HttpResult(val code: Int, val text: String) {
        val apiMessage: String? = runCatching { JSONObject(text).getJSONObject("error").optString("message") }.getOrNull()
        val isModelMissing: Boolean
            get() {
                val m = text.lowercase()
                return code == 404 || (code == 400 && "model" in m &&
                    ("decommission" in m || "does not exist" in m || "not found" in m || "not a valid model" in m))
            }
    }

    private fun extractJson(content: String): JSONObject? {
        val start = content.indexOf('{')
        val end = content.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return runCatching { JSONObject(content.substring(start, end + 1)) }.getOrNull()
    }

    companion object {
        private val nonChat = listOf("whisper", "tts", "guard", "orpheus", "embed", "safeguard", "playai", "distil", "moderation", "image", "audio", "transcribe")
        fun isChatModel(id: String) = id.isNotBlank() && nonChat.none { id.lowercase().contains(it) }
    }
}
