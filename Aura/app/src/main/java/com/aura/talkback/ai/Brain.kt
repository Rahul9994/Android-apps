package com.aura.talkback.ai

import com.aura.talkback.data.AuraSettings
import com.aura.talkback.data.ChatMessage
import com.aura.talkback.data.DayUsage
import com.aura.talkback.data.Memory
import com.aura.talkback.data.Nudge
import com.aura.talkback.data.Role
import com.aura.talkback.data.Tone
import org.json.JSONArray
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

data class NudgeContext(
    val app: String,
    val sessionMin: Int,
    val todayAppMin: Int,
    val notifications: Int,
    val night: Boolean,
    val memories: List<Memory>,
    val todaysNudges: List<Nudge>,
    val snoozesForApp: Int,
)

data class NudgeReply(val message: String, val lockMinutes: Int, val fromAi: Boolean)

data class AgentAction(val type: String, val app: String, val minutes: Int)

data class AgentReply(val reply: String, val remember: List<String>, val actions: List<AgentAction>)

/** All of Aura's prompts, plus offline fallbacks so it still talks back without a key. */
class Brain(private val ai: AiClient) {

    suspend fun nudge(s: AuraSettings, c: NudgeContext): NudgeReply {
        if (!s.hasAi) return templateNudge(s, c)
        val system = """
            You are Aura, an AI that lives in ${s.displayName}'s phone and talks back when they're losing time to their phone.
            Personality: ${s.tone.prompt}.
            Write ONE message (max 40 words) that pops up over the app they're using and is read aloud.
            Rules:
            - Address them by name. Say the exact app and minutes.
            - If a memory shows a goal, plan or promise, reference it naturally WITH when they said it
              (e.g. "You told me yesterday you wanted to study DSA").
            - If they already asked for more time today, call it out.
            - Late at night, mention sleep.
            - End by offering to lock the app for a specific number of minutes, as a question.
            - Choose lock_minutes between 10 and 120 (longer at night). Never preach, never mention being an AI.
            Reply with ONLY JSON: {"message":"...","lock_minutes":30}
        """.trimIndent()
        val user = buildString {
            appendLine("Now: ${nowLabel()}${if (c.night) " (late night)" else ""}")
            appendLine("App: ${c.app}. Current session: ${c.sessionMin} min. Total in ${c.app} today: ${c.todayAppMin} min.")
            if (c.notifications > 0) appendLine("${c.app} sent ${c.notifications} notifications today.")
            if (c.snoozesForApp > 0) appendLine("They already asked for 'just 5 more minutes' ${c.snoozesForApp} time(s) today.")
            appendLine("What they've told you (newest first):")
            if (c.memories.isEmpty()) appendLine("- nothing yet") else c.memories.take(12).forEach { appendLine("- (${relative(it.ts)}) ${it.text}") }
            if (c.todaysNudges.isNotEmpty()) {
                appendLine("Earlier nudges today:")
                c.todaysNudges.take(5).forEach { appendLine("- ${timeOf(it.ts)} ${it.appLabel} after ${it.sessionMin} min → ${it.outcome.label}") }
            }
        }
        return try {
            val json = ai.chatJson(s, listOf(ChatTurn("system", system), ChatTurn("user", user)), 0.8)
            val msg = json.optString("message").trim()
            if (msg.isBlank()) templateNudge(s, c)
            else NudgeReply(msg, json.optInt("lock_minutes", s.defaultLockMin).coerceIn(5, 180), true)
        } catch (e: AiException) {
            templateNudge(s, c)
        }
    }

    suspend fun chat(
        s: AuraSettings,
        history: List<ChatMessage>,
        userText: String,
        memories: List<Memory>,
        usage: DayUsage?,
        watchedLabels: List<String>,
        lockLabels: List<String>,
        recentNudges: List<Nudge>,
    ): AgentReply {
        if (!s.hasAi) return offlineChat(userText)
        val system = """
            You are Aura, the AI that lives in ${s.displayName}'s phone. Personality: ${s.tone.prompt}.
            You watch which apps they use, talk back when they doom-scroll, and remember what they tell you.
            You can act, but only when they ask or clearly agree:
            - lock an app for N minutes, unlock an app, start or stop watching an app.
            Save durable facts (goals, plans, deadlines, promises, schedule, preferences) in "remember" as short
            third-person notes, e.g. "Wants to study DSA every night at 9 PM". Don't re-save known facts.
            Keep replies short (max 60 words), conversational and specific to their real data: write times like "5h 18m",
            and when they ask about their usage, give one honest observation and one suggestion instead of listing every number. No markdown.
            Reply with ONLY JSON:
            {"reply":"...","remember":["..."],"actions":[{"type":"lock|unlock|watch|unwatch","app":"Instagram","minutes":30}]}
        """.trimIndent()
        val context = buildString {
            appendLine("Now: ${nowLabel()}")
            if (usage != null) {
                appendLine("Screen time today: ${dur(usage.totalMs)}, unlocks: ${usage.unlocks}, in distracting apps: ${dur(usage.watchedMs)}.")
                appendLine("Top apps today: " + usage.apps.take(6).joinToString { "${it.label} ${dur(it.ms)}" })
            }
            appendLine("Apps you watch: ${watchedLabels.joinToString().ifBlank { "none" }}")
            appendLine("Currently locked: ${lockLabels.joinToString().ifBlank { "none" }}")
            appendLine("Memories (newest first):")
            if (memories.isEmpty()) appendLine("- none") else memories.take(25).forEach { appendLine("- (${relative(it.ts)}) ${it.text}") }
            if (recentNudges.isNotEmpty()) {
                appendLine("Recent talk-backs:")
                recentNudges.take(6).forEach { appendLine("- ${relative(it.ts)}: ${it.appLabel} after ${it.sessionMin} min → ${it.outcome.label}") }
            }
        }
        val turns = buildList {
            add(ChatTurn("system", system))
            add(ChatTurn("system", context))
            history.takeLast(14).forEach { add(ChatTurn(if (it.role == Role.USER) "user" else "assistant", it.text)) }
            add(ChatTurn("user", userText))
        }
        return try {
            val json = ai.chatJson(s, turns, 0.7)
            AgentReply(
                reply = json.optString("reply").ifBlank { "I'm here." },
                remember = json.optJSONArray("remember").strings(),
                actions = json.optJSONArray("actions")?.let { arr ->
                    (0 until arr.length()).mapNotNull { i ->
                        val o = arr.optJSONObject(i) ?: return@mapNotNull null
                        AgentAction(o.optString("type").lowercase(), o.optString("app"), o.optInt("minutes", s.defaultLockMin))
                    }
                } ?: emptyList(),
            )
        } catch (e: AiException) {
            offlineChat(userText).copy(reply = "${e.message} (I've still noted what you said.)")
        }
    }

    suspend fun insight(s: AuraSettings, usage: DayUsage, watchedLabels: List<String>, memories: List<Memory>): String? {
        if (!s.hasAi || usage.apps.isEmpty()) return null
        val system = """
            You are Aura, ${s.displayName}'s phone companion. Personality: ${s.tone.prompt}.
            Write 2 short sentences (max 32 words total) about their phone use today with one specific, doable suggestion.
            Reference their goals if relevant. Reply with ONLY JSON: {"insight":"..."}
        """.trimIndent()
        val user = buildString {
            appendLine("Now: ${nowLabel()}")
            appendLine("Screen time ${dur(usage.totalMs)}, ${usage.unlocks} unlocks, distracting apps ${dur(usage.watchedMs)}.")
            appendLine("Top: " + usage.apps.take(5).joinToString { "${it.label} ${dur(it.ms)}" })
            appendLine("Distracting apps: ${watchedLabels.joinToString()}")
            memories.take(6).forEach { appendLine("Memory: ${it.text}") }
        }
        return runCatching {
            ai.chatJson(s, listOf(ChatTurn("system", system), ChatTurn("user", user)), 0.7).optString("insight").ifBlank { null }
        }.getOrNull()
    }

    // ---- Offline fallbacks -----------------------------------------------------------------

    fun templateNudge(s: AuraSettings, c: NudgeContext): NudgeReply {
        val name = s.displayName
        val goal = c.memories.firstOrNull()
        val lock = if (c.night) maxOf(60, s.defaultLockMin) else s.defaultLockMin
        val goalLine = goal?.let { " You told me ${relative(it.ts)}: “${it.text}”." } ?: ""
        val msg = when (s.tone) {
            Tone.FRIEND -> when {
                c.snoozesForApp > 0 -> "$name, that was supposed to be 5 more minutes. You're at ${c.sessionMin} on ${c.app}.$goalLine Want me to lock it for $lock minutes?"
                c.night -> "$name, it's ${timeOf(System.currentTimeMillis())} and you've been on ${c.app} for ${c.sessionMin} minutes.$goalLine Sleep might feel better. Lock it for $lock minutes?"
                else -> "$name, you've been on ${c.app} for ${c.sessionMin} minutes.$goalLine Want me to lock it for $lock minutes?"
            }
            Tone.COACH -> "${c.sessionMin} minutes on ${c.app}, $name.$goalLine That time was meant for something better. Lock it for $lock and get moving?"
            Tone.SAVAGE -> "$name. ${c.sessionMin} minutes of ${c.app}. The algorithm is winning.$goalLine Shall I lock it for $lock minutes before it eats your night?"
        }
        return NudgeReply(msg, lock, false)
    }

    private fun offlineChat(text: String): AgentReply {
        val t = text.lowercase()
        val lock = Regex("""lock\s+([a-z0-9 .]+?)\s+(?:for\s+)?(\d+)\s*(min|minutes|m|hour|hours|h)""").find(t)
        val actions = lock?.let {
            val n = it.groupValues[2].toInt()
            val mins = if (it.groupValues[3].startsWith("h")) n * 60 else n
            listOf(AgentAction("lock", it.groupValues[1].trim(), mins))
        } ?: emptyList()
        val goalWords = listOf("want", "need to", "goal", "plan", "study", "i will", "i'll", "exam", "deadline", "trying to")
        val remember = if (goalWords.any { it in t }) listOf(text.trim()) else emptyList()
        val reply = when {
            actions.isNotEmpty() -> "Done."
            remember.isNotEmpty() -> "Noted — I'll remind you of that when you're scrolling. Add an AI key in Settings and I can talk properly."
            else -> "I can lock apps and remember your goals even offline. Add a Groq or OpenRouter key in Settings to really talk with me."
        }
        return AgentReply(reply, remember, actions)
    }

    companion object {
        fun minutes(ms: Long) = (ms / 60_000.0).roundToInt()

        /** "5h 18m" / "42m" */
        fun dur(ms: Long): String { val m = minutes(ms); return if (m >= 60) "${m / 60}h ${m % 60}m" else "${m}m" }

        fun timeOf(ts: Long): String =
            Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))

        fun nowLabel(): String =
            java.time.ZonedDateTime.now().format(DateTimeFormatter.ofPattern("EEEE h:mm a", Locale.ENGLISH))

        /** "today at 9:14 PM", "yesterday", "on Monday", "on 3 Oct". */
        fun relative(ts: Long): String {
            val z = Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault())
            val days = LocalDate.now().toEpochDay() - z.toLocalDate().toEpochDay()
            return when {
                days <= 0L -> "today at ${timeOf(ts)}"
                days == 1L -> "yesterday"
                days < 7 -> "on ${z.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)}"
                else -> "on ${z.format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH))}"
            }
        }
    }
}

private fun JSONArray?.strings(): List<String> =
    if (this == null) emptyList() else (0 until length()).map { optString(it).trim() }.filter { it.isNotBlank() }
