package com.chagil.app.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Speaks instructions in Korean with the platform TextToSpeech. Safe when TTS is missing:
 * [available] becomes false and [speak] does nothing. Never speaks while [muted].
 */
class VoiceGuide(context: Context) {

    private val _muted = MutableStateFlow(false)
    val muted: StateFlow<Boolean> = _muted.asStateFlow()

    /** null = still starting, true = ready, false = no Korean TTS on this device. */
    private val _available = MutableStateFlow<Boolean?>(null)
    val available: StateFlow<Boolean?> = _available.asStateFlow()

    @Volatile private var ready = false
    @Volatile private var pending: String? = null
    private var counter = 0

    private val tts: TextToSpeech? = try {
        TextToSpeech(context.applicationContext) { status -> onInit(status) }
    } catch (_: Throwable) {
        _available.value = false
        null
    }

    private fun onInit(status: Int) {
        val engine = tts
        if (status != TextToSpeech.SUCCESS || engine == null) {
            _available.value = false
            return
        }
        val result = try {
            engine.setLanguage(Locale.KOREAN)
        } catch (_: Throwable) {
            TextToSpeech.LANG_NOT_SUPPORTED
        }
        ready = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        _available.value = ready
        pending?.let { pending = null; speak(it) }
    }

    /** Speaks [text]. [interrupt] = true drops whatever is being said (default: newest instruction wins). */
    fun speak(text: String?, interrupt: Boolean = true) {
        if (text.isNullOrBlank() || _muted.value) return
        if (!ready) {
            if (_available.value == null) pending = text
            return
        }
        try {
            tts?.speak(text, if (interrupt) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, "chagil-${counter++}")
        } catch (_: Throwable) {
        }
    }

    fun stop() {
        pending = null
        try {
            tts?.stop()
        } catch (_: Throwable) {
        }
    }

    fun setMuted(muted: Boolean) {
        _muted.value = muted
        if (muted) stop()
    }

    fun toggleMute() = setMuted(!_muted.value)

    fun shutdown() {
        stop()
        try {
            tts?.shutdown()
        } catch (_: Throwable) {
        }
        ready = false
    }
}
