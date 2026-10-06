package com.aura.talkback.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class AppUsage(val pkg: String, val label: String, val ms: Long)

data class DayUsage(
    val apps: List<AppUsage>,
    val totalMs: Long,
    val unlocks: Int,
    /** Minutes spent in watched apps per hour of the day (24 buckets). */
    val watchedByHour: FloatArray,
    val watchedMs: Long,
)

/** Screen time from the system's usage events. Needs Usage Access. */
class UsageRepo(private val context: Context, private val catalog: AppCatalog) {
    private val usm = context.getSystemService(UsageStatsManager::class.java)

    suspend fun today(watched: Set<String>): DayUsage = withContext(Dispatchers.IO) { compute(watched) }

    fun compute(watched: Set<String>): DayUsage {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        val totals = HashMap<String, Long>()
        val byHour = FloatArray(24)
        var unlocks = 0
        var fgPkg: String? = null
        var fgStart = 0L

        fun close(at: Long) {
            val pkg = fgPkg ?: return
            if (at > fgStart) {
                totals[pkg] = (totals[pkg] ?: 0L) + (at - fgStart)
                if (pkg in watched) addToHours(byHour, fgStart, at, zone)
            }
            fgPkg = null
        }

        val events = runCatching { usm.queryEvents(start, now) }.getOrNull()
            ?: return DayUsage(emptyList(), 0, 0, byHour, 0)
        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            when (e.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    close(e.timeStamp)
                    fgPkg = e.packageName
                    fgStart = e.timeStamp
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> if (e.packageName == fgPkg) close(e.timeStamp)
                else -> {
                    if (Build.VERSION.SDK_INT >= 28) {
                        if (e.eventType == UsageEvents.Event.SCREEN_NON_INTERACTIVE) close(e.timeStamp)
                        if (e.eventType == UsageEvents.Event.KEYGUARD_HIDDEN) unlocks++
                    }
                }
            }
        }
        close(now)

        val home = catalog.homePackages
        val apps = totals
            .filterKeys { it != context.packageName && it !in home && catalog.isLaunchable(it) }
            .map { (pkg, ms) -> AppUsage(pkg, catalog.label(pkg), ms) }
            .filter { it.ms >= 30_000 }
            .sortedByDescending { it.ms }
        return DayUsage(
            apps = apps,
            totalMs = apps.sumOf { it.ms },
            unlocks = unlocks,
            watchedByHour = byHour,
            watchedMs = apps.filter { it.pkg in watched }.sumOf { it.ms },
        )
    }

    private fun addToHours(buckets: FloatArray, from: Long, to: Long, zone: ZoneId) {
        var t = from
        while (t < to) {
            val z = Instant.ofEpochMilli(t).atZone(zone)
            val hourEnd = z.withMinute(0).withSecond(0).withNano(0).plusHours(1).toInstant().toEpochMilli()
            val end = minOf(to, hourEnd)
            buckets[z.hour] += (end - t) / 60_000f
            t = end
        }
    }
}
