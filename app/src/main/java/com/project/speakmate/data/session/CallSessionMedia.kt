package com.project.speakmate.data.session

import com.project.speakmate.data.audio.PlaybackProgress
import com.project.speakmate.data.remote.ai.GeminiLiveEvent
import com.project.speakmate.domain.model.CallFailure
import com.project.speakmate.domain.model.Scenario
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

interface CallSessionMedia {
    fun hasMicrophonePermission(): Boolean
    fun connect(scenario: Scenario, onEvent: (GeminiLiveEvent) -> Unit, onFailure: (CallFailure) -> Unit)
    fun reconnect(
        scenario: Scenario,
        resumptionHandle: String,
        onEvent: (GeminiLiveEvent) -> Unit,
        onFailure: (CallFailure) -> Unit
    )
    fun getConnectionFailure(): CallFailure?
    suspend fun awaitTurnBoundary()
    fun openAudio(
        onFailure: (CallFailure) -> Unit,
        onInterrupted: () -> Unit,
        onMicrophoneSilenced: (Boolean) -> Unit
    ): StateFlow<Float>
    fun startPlayback(scope: CoroutineScope, isMediaCurrent: () -> Boolean, onPlaybackChanged: (Boolean) -> Unit)
    fun enableDownlink(generation: Long, requiresIdleTurn: Boolean): CallFailure?
    fun enqueuePlayback(chunk: ByteArray): Boolean
    fun markPlaybackTurn()
    fun clearPlayback(): PlaybackProgress
    fun setPlaybackPaused(isPaused: Boolean)
    fun speakPronunciation(text: String, onFinished: (Boolean) -> Unit)
    fun stopPronunciation()
    fun enableUplink(scope: CoroutineScope, isMediaCurrent: () -> Boolean)
    fun suspendUplink()
    fun blockUplink()
    fun finishAudioInput()
    fun suspendMedia()
    fun closeAudio()
    fun close()
}
