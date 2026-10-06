package com.aura.talkback.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Locale

/**
 * Aura's voice. Plays on the media volume (so it works on vibrate) as an assistant voice,
 * and briefly ducks whatever else is playing — like a reel's audio — while it speaks.
 */
class Voice(context: Context) {
    private val app = context.applicationContext
    private val audio = app.getSystemService(AudioManager::class.java)
    val speaking = MutableStateFlow(false)
    private var ready = false
    private var pending: String? = null

    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANT)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attributes)
        .setOnAudioFocusChangeListener { }
        .build()

    private val tts: TextToSpeech = TextToSpeech(app) { status ->
        if (status == TextToSpeech.SUCCESS) {
            ready = true
            val india = Locale.forLanguageTag("en-IN")
            tts.language = if (tts.isLanguageAvailable(india) >= TextToSpeech.LANG_AVAILABLE) india else Locale.US
            tts.setSpeechRate(1.02f)
            tts.setAudioAttributes(attributes)
            pending?.let { speakNow(it) }
            pending = null
        }
    }.also {
        it.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { speaking.value = true }
            override fun onDone(utteranceId: String?) = finished()
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = finished()
            override fun onError(utteranceId: String?, errorCode: Int) = finished()
        })
    }

    private fun finished() {
        speaking.value = false
        audio.abandonAudioFocusRequest(focus)
    }

    /** True when the media volume is above zero, i.e. the user can actually hear Aura. */
    fun audible(): Boolean = audio.getStreamVolume(AudioManager.STREAM_MUSIC) > 0

    /** Returns false (and stays quiet) when media volume is at zero. */
    fun speak(text: String): Boolean {
        if (!audible()) return false
        if (!ready) { pending = text; return true }
        speakNow(text)
        return true
    }

    private fun speakNow(text: String) {
        audio.requestAudioFocus(focus)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "aura-${System.currentTimeMillis()}")
    }

    fun stop() {
        pending = null
        tts.stop()
        finished()
    }

    fun shutdown() {
        stop()
        tts.shutdown()
    }
}
