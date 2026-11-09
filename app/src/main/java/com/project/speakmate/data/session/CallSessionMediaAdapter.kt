package com.project.speakmate.data.session

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.project.speakmate.data.audio.AudioSessionManager
import com.project.speakmate.data.audio.PlaybackProgress
import com.project.speakmate.data.audio.PronunciationPlayer
import com.project.speakmate.data.remote.ai.GeminiLiveClient
import com.project.speakmate.data.remote.ai.GeminiLiveEvent
import com.project.speakmate.domain.model.CallFailure
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.utils.AppConstant
import com.project.speakmate.utils.extension.default
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient

class CallSessionMediaAdapter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val httpClient: OkHttpClient,
    @param:Named(AppConstant.GEMINI_API_KEY) private val apiKey: String
) : CallSessionMedia {
    private var transport: GeminiLiveClient? = null
    private var audio: AudioSessionManager? = null
    private var pronunciation: PronunciationPlayer? = null

    override fun hasMicrophonePermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    override fun connect(scenario: Scenario, onEvent: (GeminiLiveEvent) -> Unit, onFailure: (CallFailure) -> Unit) {
        val client = GeminiLiveClient(httpClient, apiKey, onEvent, onFailure)
        transport = client
        client.connect(scenario)
    }

    override fun reconnect(
        scenario: Scenario,
        resumptionHandle: String,
        onEvent: (GeminiLiveEvent) -> Unit,
        onFailure: (CallFailure) -> Unit
    ) {
        transport?.close()
        val client = GeminiLiveClient(httpClient, apiKey, onEvent, onFailure)
        transport = client
        client.connect(scenario, resumptionHandle)
    }

    override fun getConnectionFailure(): CallFailure? {
        val client = transport ?: return CallFailure.NETWORK
        return client.getConnectionFailure()
    }

    override suspend fun awaitTurnBoundary() {
        checkNotNull(transport).isTurnPending.first { !it }
    }

    override fun openAudio(
        onFailure: (CallFailure) -> Unit,
        onInterrupted: () -> Unit,
        onMicrophoneSilenced: (Boolean) -> Unit
    ): StateFlow<Float> {
        val manager = AudioSessionManager(context, onFailure, onInterrupted, onMicrophoneSilenced)
        audio = manager
        manager.open()
        return manager.audioLevel
    }

    override fun startPlayback(scope: CoroutineScope, isMediaCurrent: () -> Boolean, onPlaybackChanged: (Boolean) -> Unit) =
        checkNotNull(audio).startPlayback(scope, isMediaCurrent, onPlaybackChanged)

    override fun enableDownlink(generation: Long, requiresIdleTurn: Boolean): CallFailure? {
        val client = transport ?: return CallFailure.NETWORK
        return client.enableDownlink(generation, requiresIdleTurn)
    }

    override fun enqueuePlayback(chunk: ByteArray): Boolean = audio?.enqueue(chunk) == true

    override fun markPlaybackTurn() {
        audio?.markPlaybackTurn()
    }

    override fun setPlaybackPaused(isPaused: Boolean) {
        audio?.setPlaybackPaused(isPaused)
    }

    override fun speakPronunciation(text: String, onFinished: (Boolean) -> Unit) {
        val player = pronunciation ?: PronunciationPlayer(context).also { pronunciation = it }
        player.speak(text, onFinished)
    }

    override fun stopPronunciation() {
        pronunciation?.stop()
    }

    override fun clearPlayback(): PlaybackProgress = audio?.clearPlayback().default(PlaybackProgress(0L, 0L))

    override fun enableUplink(scope: CoroutineScope, isMediaCurrent: () -> Boolean) {
        if (!isMediaCurrent()) return
        val client = checkNotNull(transport)
        val generation = client.enableAudio() ?: return
        checkNotNull(audio).startCapture(scope, isMediaCurrent) { chunk, hasAudibleInput ->
            client.sendAudio(generation, chunk, hasAudibleInput)
        }
    }

    override fun suspendUplink() {
        transport?.suspendAudio()
        audio?.stopCapture()
    }

    override fun blockUplink() {
        transport?.suspendAudio()
    }

    override fun finishAudioInput() {
        audio?.clearMicrophoneLevel()
        transport?.finishAudioInput()
    }

    override fun suspendMedia() {
        transport?.suspendMedia()
    }

    override fun closeAudio() {
        val manager = audio
        audio = null
        manager?.close()
    }

    override fun close() {
        val client = transport
        val player = pronunciation
        transport = null
        pronunciation = null
        try {
            player?.release()
            closeAudio()
        } finally {
            client?.close()
        }
    }
}
