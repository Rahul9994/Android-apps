package com.aura.talkback.service

import kotlinx.coroutines.flow.MutableStateFlow

data class Foreground(val pkg: String, val label: String, val since: Long)

/** Live state shared between the background service and the app UI (same process). */
object AuraLive {
    val serviceOn = MutableStateFlow(false)
    val foreground = MutableStateFlow<Foreground?>(null)
}
