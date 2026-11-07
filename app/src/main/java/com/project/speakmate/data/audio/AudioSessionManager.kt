package com.project.speakmate.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import com.project.speakmate.domain.model.CallFailure
import com.project.speakmate.utils.AppConstant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class AudioSessionManager(
    context: Context,
    private val onFailure: (CallFailure) -> Unit,
    private val onInterrupted: () -> Unit
) {
    companion object {
        private const val PCM16_MAX_AMPLITUDE = 32_768f
        private const val LEVEL_GAIN = 4f
        private const val UNSIGNED_BYTE_MASK = 0xff
        private const val MIN_AUDIBLE_INPUT_LEVEL = 0.02f
    }

    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val playback = AudioPlayback()
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        .setAudioAttributes(attributes)
        .setOnAudioFocusChangeListener(::handleFocusChange)
        .build()
    private val mutableAudioLevel = MutableStateFlow(0f)
    val audioLevel = mutableAudioLevel.asStateFlow()
    private var hasFocus = false
    private var captureJob: Job? = null
    private var playbackJob: Job? = null
    @Volatile
    private var isPlaying = false

    fun open() {
        hasFocus = checkNotNull(audioManager).requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        check(hasFocus)
    }

    fun startCapture(
        scope: CoroutineScope,
        isMediaCurrent: () -> Boolean,
        sendAudio: (ByteArray, Boolean) -> Boolean
    ) {
        if (!isMediaCurrent()) return
        val previousJob = captureJob
        captureJob = scope.launch(Dispatchers.IO) {
            previousJob?.cancelAndJoin()
            runAudio(isMediaCurrent) {
                try {
                    AudioCapture().record { chunk -> sendMicrophoneChunk(chunk, isMediaCurrent, sendAudio) }
                } finally {
                    clearMicrophoneLevel()
                }
            }
        }
    }

    fun stopCapture() {
        captureJob?.cancel()
        captureJob = null
    }

    fun startPlayback(scope: CoroutineScope, isMediaCurrent: () -> Boolean, onPlaybackChanged: (Boolean) -> Unit) {
        playbackJob = scope.launch(Dispatchers.IO) {
            runAudio(isMediaCurrent) {
                playback.play(attributes, isMediaCurrent, ::getLevel) { isPlaying, level ->
                    if (publishPlaybackLevel(isPlaying, level)) onPlaybackChanged(isPlaying)
                }
            }
        }
    }

    fun enqueue(chunk: ByteArray): Boolean = playback.enqueue(chunk)

    fun clearMicrophoneLevel() {
        if (!isPlaying) mutableAudioLevel.value = 0f
    }

    fun close() {
        stopCapture()
        playbackJob?.cancel()
        playbackJob = null
        playback.close()
        if (hasFocus) {
            hasFocus = false
            runCatching { audioManager?.abandonAudioFocusRequest(focusRequest) }
        }
    }

    private fun handleFocusChange(change: Int) {
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> onFailure(CallFailure.AUDIO)
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> onInterrupted()
        }
    }

    private fun sendMicrophoneChunk(chunk: ByteArray, isMediaCurrent: () -> Boolean, sendAudio: (ByteArray, Boolean) -> Boolean) {
        if (!isMediaCurrent()) return
        val level = getLevel(chunk)
        val isUserAudible = !isPlaying && level >= MIN_AUDIBLE_INPUT_LEVEL
        if (sendAudio(chunk, isUserAudible) && !isPlaying) mutableAudioLevel.value = level
    }

    private fun publishPlaybackLevel(isPlaying: Boolean, level: Float): Boolean {
        val hasChanged = this.isPlaying != isPlaying
        if (isPlaying || hasChanged) mutableAudioLevel.value = level
        this.isPlaying = isPlaying
        return hasChanged
    }

    private suspend fun runAudio(isMediaCurrent: () -> Boolean, operation: suspend () -> Unit) {
        try {
            operation()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: SecurityException) {
            if (isMediaCurrent()) onFailure(CallFailure.MICROPHONE_PERMISSION)
        } catch (_: Exception) {
            if (isMediaCurrent()) onFailure(CallFailure.AUDIO)
        }
    }

    private fun getLevel(chunk: ByteArray): Float {
        var squares = 0.0
        for (index in chunk.indices step AppConstant.PCM16_BYTES_PER_SAMPLE) {
            val sample = (chunk[index].toInt() and UNSIGNED_BYTE_MASK) or
                (chunk[index + 1].toInt() shl AppConstant.BITS_PER_BYTE)
            squares += sample.toDouble() * sample
        }
        val samples = chunk.size / AppConstant.PCM16_BYTES_PER_SAMPLE
        return (sqrt(squares / samples).toFloat() / PCM16_MAX_AMPLITUDE * LEVEL_GAIN).coerceIn(0f, 1f)
    }
}
