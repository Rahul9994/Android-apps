package com.proteinpulse.app.work

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.proteinpulse.app.MainActivity
import com.proteinpulse.app.ProteinApp
import com.proteinpulse.app.R
import java.time.LocalDate
import kotlin.math.roundToInt

class DailyReportWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as ProteinApp
        val settings = app.settings.current()
        if (!settings.reportEnabled) return Result.success()

        val granted = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED || android.os.Build.VERSION.SDK_INT < 33
        if (!granted) return Result.success()

        val meals = app.meals.queryMealsForDay(LocalDate.now().toEpochDay())
        val total = meals.sumOf { it.proteinG }.roundToInt()
        val pct = if (settings.goalG > 0) total * 100 / settings.goalG else 0

        val title = when {
            meals.isEmpty() -> "No meals logged today"
            pct >= 100 -> "Goal hit! $total g of ${settings.goalG} g protein"
            else -> "Today: $total g of ${settings.goalG} g protein ($pct%)"
        }
        val body = when {
            meals.isEmpty() -> "Log what you ate to see your protein report."
            pct >= 100 -> "Great work across ${meals.size} meals. Tap for your full report."
            else -> "${settings.goalG - total} g short. Tap for your report and tips for tomorrow."
        }

        val intent = Intent(applicationContext, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN_TAB, MainActivity.TAB_REPORT)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(applicationContext, ProteinApp.REPORT_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF6EF2A4.toInt())
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setProgress(100, pct.coerceAtMost(100), false)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext).notify(1001, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post; nothing to do.
        }
        return Result.success()
    }
}
