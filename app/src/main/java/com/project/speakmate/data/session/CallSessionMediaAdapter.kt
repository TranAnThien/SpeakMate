package com.project.speakmate.data.session

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.project.speakmate.data.audio.AudioSessionManager
import com.project.speakmate.data.remote.ai.GeminiLiveClient
import com.project.speakmate.data.remote.ai.GeminiLiveEvent
import com.project.speakmate.domain.model.CallFailure
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.utils.AppConstant
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

    override fun hasMicrophonePermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    override fun connect(scenario: Scenario, onEvent: (GeminiLiveEvent) -> Unit, onFailure: (CallFailure) -> Unit) {
        val client = GeminiLiveClient(httpClient, apiKey, onEvent, onFailure)
        transport = client
        client.connect(scenario)
    }

    override fun getConnectionFailure(): CallFailure? {
        val client = transport ?: return CallFailure.NETWORK
        return client.getConnectionFailure()
    }

    override suspend fun awaitTurnBoundary() {
        checkNotNull(transport).isTurnPending.first { !it }
    }

    override fun openAudio(onFailure: (CallFailure) -> Unit, onInterrupted: () -> Unit): StateFlow<Float> {
        val manager = AudioSessionManager(context, onFailure, onInterrupted)
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
        transport = null
        try {
            closeAudio()
        } finally {
            client?.close()
        }
    }
}
