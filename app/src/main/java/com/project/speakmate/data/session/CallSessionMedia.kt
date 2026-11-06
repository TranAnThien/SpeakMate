package com.project.speakmate.data.session

import com.project.speakmate.data.remote.ai.GeminiLiveEvent
import com.project.speakmate.domain.model.CallFailure
import com.project.speakmate.domain.model.Scenario
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

interface CallSessionMedia {
    fun hasMicrophonePermission(): Boolean
    fun connect(scenario: Scenario, onEvent: (GeminiLiveEvent) -> Unit, onFailure: (CallFailure) -> Unit)
    fun getConnectionFailure(): CallFailure?
    suspend fun awaitTurnBoundary()
    fun openAudio(onFailure: (CallFailure) -> Unit, onInterrupted: () -> Unit): StateFlow<Float>
    fun startPlayback(scope: CoroutineScope, isMediaCurrent: () -> Boolean, onPlaybackChanged: (Boolean) -> Unit)
    fun enableDownlink(generation: Long, requiresIdleTurn: Boolean): CallFailure?
    fun enqueuePlayback(chunk: ByteArray): Boolean
    fun enableUplink(scope: CoroutineScope, isMediaCurrent: () -> Boolean)
    fun suspendUplink()
    fun finishAudioInput()
    fun suspendMedia()
    fun closeAudio()
    fun close()
}
