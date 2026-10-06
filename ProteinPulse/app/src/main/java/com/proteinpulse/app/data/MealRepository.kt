package com.proteinpulse.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

private class MealDbHelper(context: Context) : SQLiteOpenHelper(context, "protein.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE meals (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                ts INTEGER NOT NULL,
                day INTEGER NOT NULL,
                description TEXT NOT NULL,
                items TEXT,
                protein REAL NOT NULL,
                calories REAL NOT NULL,
                source TEXT NOT NULL,
                tip TEXT
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_meals_day ON meals(day)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
}

/** Stores logged meals. Every write bumps [changes] so the Flow-returning queries re-run. */
class MealRepository(context: Context) {
    private val helper = MealDbHelper(context)
    private val changes = MutableStateFlow(0)

    fun mealsForDay(epochDay: Long): Flow<List<MealEntry>> =
        changes.map { queryMealsForDay(epochDay) }.flowOn(Dispatchers.IO)

    fun dailyTotals(fromDay: Long, toDay: Long): Flow<List<DayTotal>> =
        changes.map { queryDailyTotals(fromDay, toDay) }.flowOn(Dispatchers.IO)

    fun recentDescriptions(limit: Int = 8): Flow<List<MealEntry>> =
        changes.map { queryRecentDistinct(limit) }.flowOn(Dispatchers.IO)

    suspend fun insert(
        description: String,
        analysis: MealAnalysis,
        timestamp: Long = System.currentTimeMillis(),
    ): Long = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("ts", timestamp)
            put("day", epochDayOf(timestamp))
            put("description", description)
            put("items", analysis.items.toJson())
            put("protein", analysis.proteinG)
            put("calories", analysis.calories)
            put("source", analysis.source.name)
            put("tip", analysis.tip)
        }
        helper.writableDatabase.insert("meals", null, values).also { changes.value++ }
    }

    suspend fun restore(entry: MealEntry) {
        insert(entry.description, MealAnalysis(entry.items, entry.tip, entry.source), entry.timestamp)
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        helper.writableDatabase.delete("meals", "id = ?", arrayOf(id.toString()))
        changes.value++
    }

    fun queryMealsForDay(epochDay: Long): List<MealEntry> =
        helper.readableDatabase.rawQuery(
            "SELECT * FROM meals WHERE day = ? ORDER BY ts DESC",
            arrayOf(epochDay.toString()),
        ).use { c -> buildList { while (c.moveToNext()) add(c.toEntry()) } }

    fun queryDailyTotals(fromDay: Long, toDay: Long): List<DayTotal> {
        val byDay = helper.readableDatabase.rawQuery(
            "SELECT day, SUM(protein), SUM(calories), COUNT(*) FROM meals WHERE day BETWEEN ? AND ? GROUP BY day",
            arrayOf(fromDay.toString(), toDay.toString()),
        ).use { c ->
            buildMap {
                while (c.moveToNext()) {
                    put(c.getLong(0), DayTotal(c.getLong(0), c.getDouble(1), c.getDouble(2), c.getInt(3)))
                }
            }
        }
        return (fromDay..toDay).map { day -> byDay[day] ?: DayTotal(day, 0.0, 0.0, 0) }
    }

    private fun queryRecentDistinct(limit: Int): List<MealEntry> {
        val seen = mutableSetOf<String>()
        return helper.readableDatabase.rawQuery("SELECT * FROM meals ORDER BY ts DESC LIMIT 60", null)
            .use { c ->
                buildList {
                    while (c.moveToNext() && size < limit) {
                        val entry = c.toEntry()
                        if (seen.add(entry.description.trim().lowercase())) add(entry)
                    }
                }
            }
    }

    private fun Cursor.toEntry() = MealEntry(
        id = getLong(getColumnIndexOrThrow("id")),
        timestamp = getLong(getColumnIndexOrThrow("ts")),
        epochDay = getLong(getColumnIndexOrThrow("day")),
        description = getString(getColumnIndexOrThrow("description")),
        items = parseFoodItems(getString(getColumnIndexOrThrow("items"))),
        proteinG = getDouble(getColumnIndexOrThrow("protein")),
        calories = getDouble(getColumnIndexOrThrow("calories")),
        source = runCatching { AnalysisSource.valueOf(getString(getColumnIndexOrThrow("source"))) }
            .getOrDefault(AnalysisSource.OFFLINE),
        tip = getString(getColumnIndexOrThrow("tip")),
    )

    companion object {
        fun epochDayOf(timestamp: Long): Long =
            Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
    }
}
