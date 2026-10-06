package com.aura.talkback

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.aura.talkback.ai.AiClient
import com.aura.talkback.ai.Brain
import com.aura.talkback.data.AppCatalog
import com.aura.talkback.data.AuraDb
import com.aura.talkback.data.SettingsStore
import com.aura.talkback.data.UsageRepo

class AuraApp : Application() {
    lateinit var settings: SettingsStore private set
    lateinit var db: AuraDb private set
    lateinit var catalog: AppCatalog private set
    lateinit var usage: UsageRepo private set
    val ai by lazy { AiClient() }
    val brain by lazy { Brain(ai) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        settings = SettingsStore(this)
        db = AuraDb(this)
        catalog = AppCatalog(this)
        usage = UsageRepo(this, catalog)
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.channel_aura), NotificationManager.IMPORTANCE_HIGH)
        )
    }

    companion object {
        const val CHANNEL = "aura"
        lateinit var instance: AuraApp private set
    }
}
