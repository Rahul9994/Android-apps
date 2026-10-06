package com.aura.talkback.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class Memory(val id: Long, val ts: Long, val text: String)

enum class Role { USER, AURA }

data class ChatMessage(val id: Long, val ts: Long, val role: Role, val text: String, val actions: List<String>)

enum class Outcome(val label: String) {
    PENDING("No answer yet"),
    LOCKED("Locked it"),
    SNOOZED("Asked for 5 more min"),
    DISMISSED("Said they're fine"),
}

data class Nudge(
    val id: Long,
    val ts: Long,
    val pkg: String,
    val appLabel: String,
    val sessionMin: Int,
    val message: String,
    val outcome: Outcome,
    val lockMin: Int,
)

private class Helper(context: Context) : SQLiteOpenHelper(context, "aura.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE memories (id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER NOT NULL, text TEXT NOT NULL)")
        db.execSQL("CREATE TABLE messages (id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER NOT NULL, role TEXT NOT NULL, text TEXT NOT NULL, actions TEXT)")
        db.execSQL(
            "CREATE TABLE nudges (id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER NOT NULL, pkg TEXT NOT NULL, label TEXT NOT NULL, " +
                "session_min INTEGER NOT NULL, message TEXT NOT NULL, outcome TEXT NOT NULL, lock_min INTEGER NOT NULL)"
        )
        db.execSQL("CREATE TABLE notif_counts (day INTEGER NOT NULL, pkg TEXT NOT NULL, count INTEGER NOT NULL, PRIMARY KEY(day, pkg))")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
}

/** Everything Aura remembers: your commitments, your chat, every time it talked back, and notification counts. */
class AuraDb(context: Context) {
    private val helper = Helper(context)
    private val changes = MutableStateFlow(0)
    private fun bump() { changes.value++ }

    // ---- Memories --------------------------------------------------------------------------
    val memories: Flow<List<Memory>> = changes.map { queryMemories() }.flowOn(Dispatchers.IO)

    fun queryMemories(limit: Int = 60): List<Memory> =
        helper.readableDatabase.rawQuery("SELECT id, ts, text FROM memories ORDER BY ts DESC LIMIT $limit", null).use { c ->
            buildList { while (c.moveToNext()) add(Memory(c.getLong(0), c.getLong(1), c.getString(2))) }
        }

    suspend fun remember(text: String) = withContext(Dispatchers.IO) {
        val clean = text.trim().take(240)
        if (clean.isEmpty()) return@withContext
        val dup = queryMemories(30).any { it.text.equals(clean, ignoreCase = true) }
        if (!dup) {
            helper.writableDatabase.insert("memories", null, ContentValues().apply {
                put("ts", System.currentTimeMillis()); put("text", clean)
            })
            bump()
        }
    }

    suspend fun forget(id: Long) = withContext(Dispatchers.IO) {
        helper.writableDatabase.delete("memories", "id = ?", arrayOf(id.toString())); bump()
    }

    // ---- Chat ------------------------------------------------------------------------------
    val messages: Flow<List<ChatMessage>> = changes.map { queryMessages() }.flowOn(Dispatchers.IO)

    fun queryMessages(limit: Int = 120): List<ChatMessage> =
        helper.readableDatabase.rawQuery(
            "SELECT id, ts, role, text, actions FROM (SELECT * FROM messages ORDER BY ts DESC LIMIT $limit) ORDER BY ts ASC", null,
        ).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        ChatMessage(
                            c.getLong(0), c.getLong(1),
                            if (c.getString(2) == "user") Role.USER else Role.AURA,
                            c.getString(3),
                            c.getString(4)?.split('\u001F')?.filter { it.isNotBlank() } ?: emptyList(),
                        )
                    )
                }
            }
        }

    suspend fun addMessage(role: Role, text: String, actions: List<String> = emptyList()): Long = withContext(Dispatchers.IO) {
        val id = helper.writableDatabase.insert("messages", null, ContentValues().apply {
            put("ts", System.currentTimeMillis())
            put("role", if (role == Role.USER) "user" else "aura")
            put("text", text)
            put("actions", actions.joinToString("\u001F"))
        })
        bump()
        id
    }

    suspend fun clearChat() = withContext(Dispatchers.IO) { helper.writableDatabase.delete("messages", null, null); bump() }

    // ---- Nudges ----------------------------------------------------------------------------
    val nudges: Flow<List<Nudge>> = changes.map { queryNudges() }.flowOn(Dispatchers.IO)

    fun queryNudges(sinceMs: Long = 0, limit: Int = 50): List<Nudge> =
        helper.readableDatabase.rawQuery(
            "SELECT id, ts, pkg, label, session_min, message, outcome, lock_min FROM nudges WHERE ts >= ? ORDER BY ts DESC LIMIT $limit",
            arrayOf(sinceMs.toString()),
        ).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        Nudge(
                            c.getLong(0), c.getLong(1), c.getString(2), c.getString(3), c.getInt(4), c.getString(5),
                            runCatching { Outcome.valueOf(c.getString(6)) }.getOrDefault(Outcome.PENDING), c.getInt(7),
                        )
                    )
                }
            }
        }

    fun addNudge(pkg: String, label: String, sessionMin: Int, message: String): Long {
        val id = helper.writableDatabase.insert("nudges", null, ContentValues().apply {
            put("ts", System.currentTimeMillis()); put("pkg", pkg); put("label", label)
            put("session_min", sessionMin); put("message", message); put("outcome", Outcome.PENDING.name); put("lock_min", 0)
        })
        bump()
        return id
    }

    fun updateNudge(id: Long, message: String? = null, outcome: Outcome? = null, lockMin: Int? = null) {
        val values = ContentValues().apply {
            message?.let { put("message", it) }
            outcome?.let { put("outcome", it.name) }
            lockMin?.let { put("lock_min", it) }
        }
        if (values.size() == 0) return
        helper.writableDatabase.update("nudges", values, "id = ?", arrayOf(id.toString()))
        bump()
    }

    // ---- Notification counts ---------------------------------------------------------------
    fun countNotification(pkg: String) {
        val day = LocalDate.now().toEpochDay()
        val db = helper.writableDatabase
        db.execSQL("INSERT OR IGNORE INTO notif_counts(day, pkg, count) VALUES(?, ?, 0)", arrayOf<Any>(day, pkg))
        db.execSQL("UPDATE notif_counts SET count = count + 1 WHERE day = ? AND pkg = ?", arrayOf<Any>(day, pkg))
        bump()
    }

    val notificationCounts: Flow<Map<String, Int>> = changes.map { queryNotificationCounts() }.flowOn(Dispatchers.IO)

    fun queryNotificationCounts(day: Long = LocalDate.now().toEpochDay()): Map<String, Int> =
        helper.readableDatabase.rawQuery("SELECT pkg, count FROM notif_counts WHERE day = ?", arrayOf(day.toString())).use { c ->
            buildMap { while (c.moveToNext()) put(c.getString(0), c.getInt(1)) }
        }

    /** Lets screens refresh things that don't live in the DB (usage stats) on the same signal. */
    val tick: Flow<Int> get() = changes
}
