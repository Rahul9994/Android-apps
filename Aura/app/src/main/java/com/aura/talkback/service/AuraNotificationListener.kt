package com.aura.talkback.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.aura.talkback.AuraApp
import java.util.concurrent.Executors

/** Counts how often each app pings you today. Only the count is stored, never the content. */
class AuraNotificationListener : NotificationListenerService() {
    private val io = Executors.newSingleThreadExecutor()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName || sbn.isOngoing) return
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val pkg = sbn.packageName
        io.execute { runCatching { AuraApp.instance.db.countNotification(pkg) } }
    }

    override fun onDestroy() {
        io.shutdown()
        super.onDestroy()
    }
}
