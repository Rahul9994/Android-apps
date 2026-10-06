package com.proteinpulse.app.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.proteinpulse.app.data.AppSettings
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object ReportScheduler {
    private const val WORK_NAME = "daily_protein_report"

    /**
     * Schedules the end-of-day report notification at the time chosen in settings.
     * [replace] = true re-anchors the schedule (use after the user changes the time).
     */
    fun schedule(context: Context, settings: AppSettings, replace: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (!settings.reportEnabled) {
            wm.cancelUniqueWork(WORK_NAME)
            return
        }
        val now = LocalDateTime.now()
        var next = now.withHour(settings.reportHour).withMinute(settings.reportMinute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)

        val request = PeriodicWorkRequestBuilder<DailyReportWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(Duration.between(now, next).toMillis(), TimeUnit.MILLISECONDS)
            .build()
        wm.enqueueUniquePeriodicWork(
            WORK_NAME,
            if (replace) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
