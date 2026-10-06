package com.proteinpulse.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.proteinpulse.app.ai.AiClient
import com.proteinpulse.app.data.MealRepository
import com.proteinpulse.app.data.SettingsStore
import com.proteinpulse.app.work.ReportScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ProteinApp : Application() {
    lateinit var meals: MealRepository
        private set
    lateinit var settings: SettingsStore
        private set
    val ai by lazy { AiClient() }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        meals = MealRepository(this)
        settings = SettingsStore(this)

        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(REPORT_CHANNEL, getString(R.string.channel_report), NotificationManager.IMPORTANCE_DEFAULT)
        )
        appScope.launch { ReportScheduler.schedule(this@ProteinApp, settings.current(), replace = false) }
    }

    companion object {
        const val REPORT_CHANNEL = "daily_report"
    }
}
