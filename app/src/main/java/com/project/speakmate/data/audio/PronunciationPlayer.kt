package com.project.speakmate.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class PronunciationPlayer(private val context: Context) {
    companion object {
        private const val UTTERANCE_PREFIX = "pronunciation_"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
    private var engine: TextToSpeech? = null
    private var isReady = false
    private var isReleased = false
    private var nextUtterance = 0L
    private var pendingText: String? = null
    private var pendingCallback: ((Boolean) -> Unit)? = null
    private var activeUtteranceId: String? = null
    private var activeCallback: ((Boolean) -> Unit)? = null

    fun speak(text: String, onFinished: (Boolean) -> Unit) {
        mainHandler.post {
            if (isReleased || text.isBlank()) {
                onFinished(false)
                return@post
            }
            stopActive()
            val current = engine
            if (current == null || !isReady) {
                pendingCallback?.invoke(false)
                pendingText = text
                pendingCallback = onFinished
                if (current == null) createEngine()
                return@post
            }
            speakNow(current, text, onFinished)
        }
    }

    fun stop() {
        mainHandler.post {
            pendingCallback = null
            pendingText = null
            stopActive()
        }
    }

    fun release() {
        mainHandler.post {
            if (isReleased) return@post
            isReleased = true
            pendingCallback = null
            pendingText = null
            stopActive()
            runCatching { engine?.shutdown() }
            engine = null
            isReady = false
        }
    }

    private fun createEngine() {
        val created = try {
            TextToSpeech(context.applicationContext) { status -> mainHandler.post { onEngineReady(status) } }
        } catch (_: RuntimeException) {
            null
        }
        if (created == null) {
            failPending()
            return
        }
        engine = created
    }

    private fun onEngineReady(status: Int) {
        val current = engine ?: return
        if (isReleased) return
        val language = if (status == TextToSpeech.SUCCESS) current.setLanguage(Locale.US) else TextToSpeech.LANG_NOT_SUPPORTED
        if (language == TextToSpeech.LANG_MISSING_DATA || language == TextToSpeech.LANG_NOT_SUPPORTED) {
            runCatching { current.shutdown() }
            engine = null
            failPending()
            return
        }
        current.setAudioAttributes(attributes)
        current.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit

            override fun onDone(utteranceId: String?) {
                mainHandler.post { completeUtterance(utteranceId, true) }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                mainHandler.post { completeUtterance(utteranceId, false) }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                mainHandler.post { completeUtterance(utteranceId, false) }
            }
        })
        isReady = true
        val text = pendingText
        val callback = pendingCallback
        pendingText = null
        pendingCallback = null
        if (text != null && callback != null) speakNow(current, text, callback)
    }

    private fun speakNow(current: TextToSpeech, text: String, onFinished: (Boolean) -> Unit) {
        val utteranceId = UTTERANCE_PREFIX + nextUtterance++
        activeUtteranceId = utteranceId
        activeCallback = onFinished
        val result = runCatching { current.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId) }
            .getOrDefault(TextToSpeech.ERROR)
        if (result != TextToSpeech.SUCCESS) completeUtterance(utteranceId, false)
    }

    private fun completeUtterance(utteranceId: String?, isSuccess: Boolean) {
        if (utteranceId == null || utteranceId != activeUtteranceId) return
        val callback = activeCallback
        activeUtteranceId = null
        activeCallback = null
        callback?.invoke(isSuccess)
    }

    private fun stopActive() {
        val callback = activeCallback
        activeUtteranceId = null
        activeCallback = null
        if (callback != null) runCatching { engine?.stop() }
    }

    private fun failPending() {
        val callback = pendingCallback
        pendingText = null
        pendingCallback = null
        callback?.invoke(false)
    }
}
