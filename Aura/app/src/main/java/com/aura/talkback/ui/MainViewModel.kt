package com.aura.talkback.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aura.talkback.AuraApp
import com.aura.talkback.ai.AiException
import com.aura.talkback.data.AppInfo
import com.aura.talkback.data.AuraSettings
import com.aura.talkback.data.ChatMessage
import com.aura.talkback.data.DayUsage
import com.aura.talkback.data.Memory
import com.aura.talkback.data.Nudge
import com.aura.talkback.data.PermissionState
import com.aura.talkback.data.Permissions
import com.aura.talkback.data.Role
import com.aura.talkback.data.Tone
import com.aura.talkback.service.AuraLive
import com.aura.talkback.service.AuraWatchService
import com.aura.talkback.service.Voice
import com.aura.talkback.ui.overlay.formatMinutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

sealed interface Async<out T> {
    data object Idle : Async<Nothing>
    data object Loading : Async<Nothing>
    data class Ready<T>(val value: T) : Async<T>
    data class Failed(val message: String) : Async<Nothing>
}

enum class Tab { HOME, TALK, APPS, YOU }

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as AuraApp

    val tab = MutableStateFlow(Tab.HOME)

    val settings: StateFlow<AuraSettings?> = app.settings.flow.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _permissions = MutableStateFlow(Permissions.check(application))
    val permissions: StateFlow<PermissionState> = _permissions.asStateFlow()

    val serviceOn = AuraLive.serviceOn
    val foreground = AuraLive.foreground

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val _usage = MutableStateFlow<DayUsage?>(null)
    val usage: StateFlow<DayUsage?> = _usage.asStateFlow()

    val memories: StateFlow<List<Memory>> = app.db.memories.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val messages: StateFlow<List<ChatMessage>> = app.db.messages.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val nudgesToday: StateFlow<List<Nudge>> = app.db.nudges.map { list ->
        val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        list.filter { it.ts >= start }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val notificationCounts: StateFlow<Map<String, Int>> =
        app.db.notificationCounts.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val thinking = MutableStateFlow(false)
    /** Id of the newest Aura reply produced in this session, so only it gets the word-by-word reveal. */
    val freshReplyAfter = MutableStateFlow(Long.MAX_VALUE)

    private val _insight = MutableStateFlow<Async<String>>(Async.Idle)
    val insight: StateFlow<Async<String>> = _insight.asStateFlow()

    private val _models = MutableStateFlow<Async<List<String>>>(Async.Idle)
    val models: StateFlow<Async<List<String>>> = _models.asStateFlow()
    val currentModel = MutableStateFlow("")

    private val _test = MutableStateFlow<Async<String>>(Async.Idle)
    val test: StateFlow<Async<String>> = _test.asStateFlow()

    val toast = MutableStateFlow<String?>(null)

    private var usageJob: Job? = null

    init {
        viewModelScope.launch { _apps.value = app.catalog.load() }
        startUsageLoop()
    }

    private fun startUsageLoop() {
        usageJob?.cancel()
        usageJob = viewModelScope.launch {
            while (isActive) {
                refreshUsage()
                delay(30_000)
            }
        }
    }

    fun onResume() {
        _permissions.value = Permissions.check(getApplication())
        app.catalog.refresh()
        viewModelScope.launch { _apps.value = app.catalog.load() }
        startUsageLoop()
    }

    suspend fun refreshUsage() {
        if (!Permissions.hasUsageAccess(getApplication())) return
        val watched = settings.value?.watched ?: app.settings.current().watched
        _usage.value = withContext(Dispatchers.IO) { runCatching { app.usage.compute(watched) }.getOrNull() }
    }

    fun update(transform: (AuraSettings) -> AuraSettings) {
        viewModelScope.launch { app.settings.update(transform) }
    }

    fun completeOnboarding(name: String, goal: String, tone: Tone, watched: Set<String>) {
        viewModelScope.launch {
            if (goal.isNotBlank()) app.db.remember(goal.trim())
            app.settings.update { it.copy(onboarded = true, userName = name.trim(), tone = tone, watched = watched) }
            refreshUsage()
        }
    }

    fun setWatched(pkg: String, on: Boolean) = update { s -> s.copy(watched = if (on) s.watched + pkg else s.watched - pkg) }

    fun lock(pkg: String, minutes: Int) = viewModelScope.launch { app.settings.lock(pkg, minutes) }
    fun unlock(pkg: String) = viewModelScope.launch { app.settings.unlock(pkg) }
    fun forget(memory: Memory) = viewModelScope.launch { app.db.forget(memory.id) }
    fun clearChat() = viewModelScope.launch { app.db.clearChat() }

    // ---- Voice -----------------------------------------------------------------------------

    private val voiceLazy = lazy { Voice(getApplication()) }
    private val voice by voiceLazy

    /** True while Aura is reading something aloud in the app. */
    val speaking: StateFlow<Boolean> get() = voice.speaking

    /** The chat message currently being read aloud, if any. */
    val speakingMessage = MutableStateFlow<Long?>(null)

    fun speak(text: String, messageId: Long? = null) {
        if (voice.speak(text)) speakingMessage.value = messageId
        else toast.value = "Your media volume is at zero. Turn it up to hear Aura."
    }

    fun stopVoice() {
        if (voiceLazy.isInitialized()) voice.stop()
        speakingMessage.value = null
    }

    fun testVoice() {
        viewModelScope.launch {
            speak("Hey ${app.settings.current().displayName}, it's Aura. This is how I'll sound when I catch you scrolling.")
        }
    }

    fun setVoice(on: Boolean) {
        if (!on) stopVoice()
        update { it.copy(voice = on) }
    }

    override fun onCleared() {
        if (voiceLazy.isInitialized()) voice.shutdown()
        super.onCleared()
    }

    fun previewTalkBack() {
        val service = AuraWatchService.running
        if (service == null) toast.value = "Turn on Aura in Accessibility settings first."
        else service.preview()
    }

    // ---- Chat agent ------------------------------------------------------------------------

    fun send(text: String) {
        val input = text.trim()
        if (input.isEmpty() || thinking.value) return
        stopVoice()
        viewModelScope.launch {
            val s = app.settings.current()
            val history = messages.value
            app.db.addMessage(Role.USER, input)
            thinking.value = true
            val labels = s.watched.map { app.catalog.label(it) }
            val locks = s.activeLocks().keys.map { app.catalog.label(it) }
            val recent = withContext(Dispatchers.IO) { app.db.queryNudges(limit = 6) }
            val reply = app.brain.chat(s, history, input, memories.value, usage.value, labels, locks, recent)

            val chips = mutableListOf<String>()
            reply.remember.forEach { app.db.remember(it); chips += "mem|$it" }
            reply.actions.forEach { a ->
                val target = app.catalog.find(a.app)
                if (target == null) { chips += "warn|Couldn't find \"${a.app}\""; return@forEach }
                when (a.type) {
                    "lock" -> {
                        val m = a.minutes.coerceIn(1, 24 * 60)
                        app.settings.lock(target.pkg, m); chips += "lock|Locked ${target.label} · ${formatMinutes(m)}"
                    }
                    "unlock" -> { app.settings.unlock(target.pkg); chips += "unlock|Unlocked ${target.label}" }
                    "watch" -> { app.settings.update { it.copy(watched = it.watched + target.pkg) }; chips += "watch|Watching ${target.label}" }
                    "unwatch" -> { app.settings.update { it.copy(watched = it.watched - target.pkg) }; chips += "unwatch|Stopped watching ${target.label}" }
                }
            }
            freshReplyAfter.value = System.currentTimeMillis() - 1
            val id = app.db.addMessage(Role.AURA, reply.reply, chips)
            thinking.value = false
            if (app.settings.current().voice) speak(reply.reply, id)
        }
    }

    // ---- Insight ---------------------------------------------------------------------------

    fun loadInsight(force: Boolean = false) {
        if (_insight.value is Async.Loading) return
        if (!force && _insight.value is Async.Ready) return
        viewModelScope.launch {
            val s = app.settings.current()
            val u = usage.value ?: run { refreshUsage(); usage.value }
            if (!s.hasAi || u == null) { _insight.value = Async.Idle; return@launch }
            _insight.value = Async.Loading
            val text = app.brain.insight(s, u, s.watched.map { app.catalog.label(it) }, memories.value)
            _insight.value = if (text != null) Async.Ready(text) else Async.Failed("Couldn't reach the AI right now.")
        }
    }

    // ---- AI engine -------------------------------------------------------------------------

    fun loadModels(refresh: Boolean = false) {
        viewModelScope.launch {
            val s = app.settings.current()
            if (!s.hasAi) { _models.value = Async.Failed("Add your ${s.provider.label} key first."); return@launch }
            _models.value = Async.Loading
            _models.value = try { Async.Ready(app.ai.listModels(s, refresh)) } catch (e: AiException) { Async.Failed(e.message ?: "Failed") }
            refreshCurrentModel()
        }
    }

    fun refreshCurrentModel() {
        viewModelScope.launch {
            val s = app.settings.current()
            currentModel.value = if (s.hasAi) runCatching { app.ai.currentModel(s) }.getOrDefault(s.provider.preferredModels.first()) else ""
        }
    }

    fun testAi() {
        _test.value = Async.Loading
        viewModelScope.launch {
            _test.value = try { Async.Ready(app.ai.test(app.settings.current())) } catch (e: AiException) { Async.Failed(e.message ?: "Failed") }
            refreshCurrentModel()
        }
    }

    fun resetAiStatus() {
        _test.value = Async.Idle
        _models.value = Async.Idle
        _insight.value = Async.Idle
    }
}
