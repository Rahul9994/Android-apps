package com.aura.talkback.service

import android.accessibilityservice.AccessibilityService
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.aura.talkback.AuraApp
import com.aura.talkback.ai.Brain
import com.aura.talkback.ai.NudgeContext
import com.aura.talkback.data.AuraSettings
import com.aura.talkback.data.Outcome
import com.aura.talkback.ui.overlay.LockOverlay
import com.aura.talkback.ui.overlay.NudgeOverlay
import com.aura.talkback.ui.overlay.NudgeUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.lang.ref.WeakReference
import java.time.LocalDate
import java.time.LocalTime

/**
 * The part of Aura that watches. It only listens for "which app is in front" window changes,
 * times the session, and when a watched app runs past your limit it slides up a talk-back overlay.
 * Locked apps are bounced to the home screen.
 */
class AuraWatchService : AccessibilityService() {
    private val app get() = application as AuraApp
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var overlay: OverlayController
    private lateinit var voice: Voice

    @Volatile private var settings = AuraSettings()
    private var current: String? = null
    private val sessionStart = HashMap<String, Long>()
    private val lastSeen = HashMap<String, Long>()
    private val snoozeUntil = HashMap<String, Long>()
    private val snoozes = HashMap<String, Int>()
    private var snoozeDay = LocalDate.now().toEpochDay()
    private var screenOn = true
    private var lastLockShown = 0L

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> { screenOn = false; setForeground(null) }
                Intent.ACTION_SCREEN_ON -> screenOn = true
                Intent.ACTION_USER_PRESENT -> { screenOn = true; detectFromUsage()?.let { onWindow(it, null) } }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = WeakReference(this)
        overlay = OverlayController(this)
        voice = Voice(this)
        scope.launch { app.settings.flow.collect { settings = it } }
        scope.launch {
            while (isActive) {
                delay(TICK_MS)
                runCatching { tick() }
            }
        }
        ContextCompat.registerReceiver(
            this, screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_USER_PRESENT)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        AuraLive.serviceOn.value = true
        detectFromUsage()?.let { onWindow(it, null) }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        onWindow(pkg, event.className?.toString())
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        AuraLive.serviceOn.value = false
        AuraLive.foreground.value = null
        runCatching { unregisterReceiver(screenReceiver) }
        if (::overlay.isInitialized) overlay.removeNow()
        if (::voice.isInitialized) voice.shutdown()
        scope.cancel()
        instance = null
        super.onDestroy()
    }

    // ---- Foreground tracking ---------------------------------------------------------------

    private fun onWindow(pkg: String, cls: String?) {
        when {
            pkg == packageName -> {
                if (!overlay.isShowing && cls?.contains("MainActivity") == true) setForeground(pkg)
            }
            pkg in app.catalog.homePackages -> setForeground(null)
            app.catalog.isLaunchable(pkg) -> {
                setForeground(pkg)
                checkLock(pkg)
            }
            // System UI, keyboards, permission dialogs etc. don't change what you're "using".
        }
    }

    private fun setForeground(pkg: String?) {
        if (pkg == current) return
        val now = System.currentTimeMillis()
        current?.let { lastSeen[it] = now }
        if (pkg != null) {
            val seen = lastSeen[pkg] ?: 0L
            if (now - seen > GRACE_MS || sessionStart[pkg] == null) sessionStart[pkg] = now
        }
        current = pkg
        AuraLive.foreground.value = pkg?.takeIf { it != packageName }?.let {
            Foreground(it, app.catalog.label(it), sessionStart[it] ?: now)
        }
    }

    /** Fallback after unlock, when no window-change event fires: ask usage stats what's in front. */
    private fun detectFromUsage(): String? = runCatching {
        val usm = getSystemService(UsageStatsManager::class.java)
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(now - 10 * 60_000, now)
        val e = UsageEvents.Event()
        var last: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) last = e.packageName
        }
        last
    }.getOrNull()

    // ---- Decisions -------------------------------------------------------------------------

    private fun tick() {
        val today = LocalDate.now().toEpochDay()
        if (today != snoozeDay) { snoozes.clear(); snoozeDay = today }
        if (!screenOn) return
        val s = settings
        val pkg = current ?: return
        val now = System.currentTimeMillis()
        lastSeen[pkg] = now
        checkLock(pkg)
        if (!s.monitoring || pkg !in s.watched || overlay.isShowing) return
        if (now < (snoozeUntil[pkg] ?: 0L)) return
        val start = sessionStart[pkg] ?: return
        val night = s.isNight(LocalTime.now().hour)
        val limitMin = if (night) s.nightNudgeAfterMin else s.nudgeAfterMin
        if (now - start >= limitMin * 60_000L) {
            talkBack(pkg, ((now - start) / 60_000L).toInt().coerceAtLeast(1), night, preview = false)
        }
    }

    private fun checkLock(pkg: String) {
        val until = settings.locks[pkg] ?: return
        val now = System.currentTimeMillis()
        if (until <= now) return
        performGlobalAction(GLOBAL_ACTION_HOME)
        if (overlay.isShowing || now - lastLockShown < 1500) return
        lastLockShown = now
        showLock(pkg, until)
    }

    private var lockShowing = false

    private fun showLock(pkg: String, until: Long) {
        val label = app.catalog.label(pkg)
        scope.launch {
            val memory = withContext(Dispatchers.IO) { app.db.queryMemories(1).firstOrNull()?.text }
            lockShowing = true
            overlay.show(onBack = { overlay.dismiss() }) { visible, onGone ->
                LockOverlay(
                    visible = visible,
                    pkg = pkg,
                    appLabel = label,
                    until = until,
                    reminder = memory,
                    name = settings.displayName,
                    onClose = { overlay.dismiss() },
                    onGone = { lockShowing = false; onGone() },
                )
            }
        }
    }

    /** Public so the app can demo the experience from Settings. */
    fun preview() {
        val s = settings
        val pkg = s.watched.firstOrNull { app.catalog.isLaunchable(it) } ?: "com.instagram.android"
        talkBack(pkg, 37, s.isNight(LocalTime.now().hour), preview = true)
    }

    private fun talkBack(pkg: String, sessionMin: Int, night: Boolean, preview: Boolean) {
        val now = System.currentTimeMillis()
        snoozeUntil[pkg] = now + 3 * 60_000L // don't stack nudges while this one is open
        val s = settings
        val label = app.catalog.label(pkg)
        val ui = MutableStateFlow(NudgeUi(pkg, label, sessionMin, message = null, lockMinutes = s.defaultLockMin, night = night))
        var nudgeId = -1L

        overlay.show(onBack = { respond(pkg, nudgeId, Outcome.DISMISSED, 0) }) { visible, onGone ->
            NudgeOverlay(
                visible = visible,
                ui = ui,
                speaking = voice.speaking,
                onLock = { minutes -> respond(pkg, nudgeId, Outcome.LOCKED, minutes) },
                onSnooze = { respond(pkg, nudgeId, Outcome.SNOOZED, 0) },
                onDismiss = { respond(pkg, nudgeId, Outcome.DISMISSED, 0) },
                onGone = onGone,
            )
        }

        scope.launch {
            val ctx = withContext(Dispatchers.IO) {
                if (!preview) nudgeId = app.db.addNudge(pkg, label, sessionMin, "")
                val usage = runCatching { app.usage.compute(s.watched) }.getOrNull()
                val startOfDay = LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                NudgeContext(
                    app = label,
                    sessionMin = sessionMin,
                    todayAppMin = usage?.apps?.firstOrNull { it.pkg == pkg }?.let { Brain.minutes(it.ms) } ?: sessionMin,
                    notifications = app.db.queryNotificationCounts()[pkg] ?: 0,
                    night = night,
                    memories = app.db.queryMemories(15),
                    todaysNudges = app.db.queryNudges(sinceMs = startOfDay, limit = 8).filter { it.id != nudgeId },
                    snoozesForApp = snoozes[pkg] ?: 0,
                )
            }
            val reply = withTimeoutOrNull(12_000) { app.brain.nudge(s, ctx) } ?: app.brain.templateNudge(s, ctx)
            ui.value = ui.value.copy(message = reply.message, lockMinutes = reply.lockMinutes)
            if (nudgeId > 0) withContext(Dispatchers.IO) { app.db.updateNudge(nudgeId, message = reply.message) }
            if (s.voice && overlay.isShowing && !voice.speak(reply.message)) {
                ui.value = ui.value.copy(voiceHint = "Your media volume is off — turn it up to hear me.")
            }
        }
    }

    private fun respond(pkg: String, nudgeId: Long, outcome: Outcome, lockMin: Int) {
        val now = System.currentTimeMillis()
        voice.stop()
        when (outcome) {
            Outcome.LOCKED -> {
                settings = settings.copy(locks = settings.locks + (pkg to now + lockMin * 60_000L))
                scope.launch { app.settings.lock(pkg, lockMin) }
                snoozes.remove(pkg)
                overlay.dismiss()
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
            Outcome.SNOOZED -> {
                snoozeUntil[pkg] = now + 5 * 60_000L
                snoozes[pkg] = (snoozes[pkg] ?: 0) + 1
                overlay.dismiss()
            }
            else -> {
                snoozeUntil[pkg] = now + maxOf(10, settings.nudgeAfterMin) * 60_000L
                sessionStart[pkg] = now
                overlay.dismiss()
            }
        }
        if (nudgeId > 0) scope.launch(Dispatchers.IO) { app.db.updateNudge(nudgeId, outcome = outcome, lockMin = lockMin) }
    }

    companion object {
        private const val TICK_MS = 5_000L
        /** Leaving an app for less than this keeps the same session going. */
        private const val GRACE_MS = 2 * 60_000L

        @Volatile private var instance: WeakReference<AuraWatchService>? = null
        val running: AuraWatchService? get() = instance?.get()
    }
}
