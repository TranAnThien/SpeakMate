package com.project.speakmate.data.session

import com.project.speakmate.data.audio.PlaybackProgress
import com.project.speakmate.data.remote.ai.GeminiLiveEvent
import com.project.speakmate.domain.model.CallFailure
import com.project.speakmate.domain.model.Scenario
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

class FakeCallSessionMedia : CallSessionMedia {
    private var onEvent: ((GeminiLiveEvent) -> Unit)? = null
    private var onFailure: ((CallFailure) -> Unit)? = null
    private var onInterrupted: (() -> Unit)? = null
    private var onMicrophoneSilenced: ((Boolean) -> Unit)? = null
    val reconnectHandles = mutableListOf<String>()
    var isUplinkBlocked = false
        private set
    var shouldFailReconnect = false
    var shouldFailPronunciation = false
    val pronouncedTexts = mutableListOf<String>()
    var isPlaybackPaused = false
        private set
    var pronunciationStops = 0
        private set
    private var onPronunciationFinished: ((Boolean) -> Unit)? = null
    val isTurnIdle = MutableStateFlow(true)
    var hasConnection = false
        private set
    var isAudioOpen = false
        private set
    private var isUplinkCurrent: (() -> Boolean)? = null
    val isUplinkEnabled: Boolean get() = isUplinkCurrent?.invoke() == true
    var hasPermission = true
    var shouldFailAudioOpen = false
    var shouldFailClose = false
    var onOpenAudio: () -> Unit = {}
    var onEnableDownlink: (Long) -> Unit = {}
    var onStartCapture: () -> Unit = {}
    var terminalFailure: CallFailure? = null
    var playbackStarts = 0
        private set
    var closeCount = 0
        private set
    var audioCloseCount = 0
        private set
    var playbackGeneration: Long? = null
        private set
    var playbackChunks = 0
        private set

    var playbackProgress = PlaybackProgress(0L, 0L)
    var clearCount = 0
        private set
    var turnMarks = 0
        private set

    override fun hasMicrophonePermission(): Boolean = hasPermission

    override fun markPlaybackTurn() {
        turnMarks++
    }

    override fun setPlaybackPaused(isPaused: Boolean) {
        isPlaybackPaused = isPaused
    }

    override fun speakPronunciation(text: String, onFinished: (Boolean) -> Unit) {
        check(!shouldFailPronunciation)
        pronouncedTexts += text
        onPronunciationFinished = onFinished
    }

    override fun stopPronunciation() {
        pronunciationStops++
    }

    fun finishPronunciation(isSuccess: Boolean) {
        onPronunciationFinished?.invoke(isSuccess)
    }

    fun capturePronunciationCallback(): (Boolean) -> Unit {
        val callback = onPronunciationFinished
        return { isSuccess -> callback?.invoke(isSuccess) }
    }

    override fun clearPlayback(): PlaybackProgress {
        clearCount++
        return playbackProgress
    }

    override fun connect(scenario: Scenario, onEvent: (GeminiLiveEvent) -> Unit, onFailure: (CallFailure) -> Unit) {
        this.onEvent = onEvent
        this.onFailure = onFailure
        hasConnection = true
    }

    override fun reconnect(
        scenario: Scenario,
        resumptionHandle: String,
        onEvent: (GeminiLiveEvent) -> Unit,
        onFailure: (CallFailure) -> Unit
    ) {
        reconnectHandles += resumptionHandle
        this.onEvent = onEvent
        this.onFailure = onFailure
        check(!shouldFailReconnect)
        hasConnection = true
    }

    override fun getConnectionFailure(): CallFailure? =
        if (hasConnection) null else terminalFailure ?: CallFailure.NETWORK

    override suspend fun awaitTurnBoundary() {
        isTurnIdle.first { it }
    }

    override fun openAudio(
        onFailure: (CallFailure) -> Unit,
        onInterrupted: () -> Unit,
        onMicrophoneSilenced: (Boolean) -> Unit
    ): MutableStateFlow<Float> {
        this.onInterrupted = onInterrupted
        this.onMicrophoneSilenced = onMicrophoneSilenced
        isAudioOpen = true
        onOpenAudio()
        check(!shouldFailAudioOpen)
        return MutableStateFlow(0f)
    }

    override fun startPlayback(scope: CoroutineScope, isMediaCurrent: () -> Boolean, onPlaybackChanged: (Boolean) -> Unit) {
        if (isMediaCurrent()) playbackStarts++
    }

    override fun enableDownlink(generation: Long, requiresIdleTurn: Boolean): CallFailure? {
        onEnableDownlink(generation)
        if (!hasConnection) return getConnectionFailure()
        if (requiresIdleTurn && !isTurnIdle.value) return CallFailure.TIMEOUT
        playbackGeneration = generation
        return null
    }

    override fun enqueuePlayback(chunk: ByteArray): Boolean {
        if (!isAudioOpen) return false
        playbackChunks++
        return true
    }

    override fun enableUplink(scope: CoroutineScope, isMediaCurrent: () -> Boolean) {
        onStartCapture()
        isUplinkBlocked = false
        isUplinkCurrent = isMediaCurrent
    }

    override fun suspendUplink() {
        isUplinkCurrent = null
    }

    override fun blockUplink() {
        isUplinkBlocked = true
    }

    override fun finishAudioInput() = Unit

    override fun suspendMedia() {
        suspendUplink()
        playbackGeneration = null
    }

    override fun closeAudio() {
        if (isAudioOpen) audioCloseCount++
        isAudioOpen = false
    }

    override fun close() {
        closeCount++
        hasConnection = false
        suspendMedia()
        closeAudio()
        check(!shouldFailClose)
    }

    fun acknowledgeSetup() {
        onEvent?.invoke(GeminiLiveEvent.SetupComplete)
    }

    fun silenceMicrophone(isSilenced: Boolean) {
        onMicrophoneSilenced?.invoke(isSilenced)
    }

    fun emitResumptionHandle(handle: String) {
        onEvent?.invoke(GeminiLiveEvent.ResumptionUpdate(handle, true))
    }

    fun interruptAudio() {
        onInterrupted?.invoke()
    }

    fun captureFailureCallback(): (CallFailure) -> Unit {
        val callback = onFailure
        return { failure -> callback?.invoke(failure) }
    }

    fun emitFailure(failure: CallFailure) {
        onFailure?.invoke(failure)
    }

    fun disconnect(failure: CallFailure) {
        hasConnection = false
        terminalFailure = failure
        emitFailure(failure)
    }

    fun emitSpeechActivity(isSpeaking: Boolean) {
        onEvent?.invoke(GeminiLiveEvent.SpeechActivity(isSpeaking, playbackGeneration))
    }

    fun emitInputTranscription(text: String, isFinished: Boolean = false) {
        onEvent?.invoke(GeminiLiveEvent.InputTranscription(text, isFinished, playbackGeneration))
    }

    fun emitOutputTranscription(text: String, generation: Long? = playbackGeneration) {
        onEvent?.invoke(GeminiLiveEvent.OutputTranscription(text, false, generation))
    }

    fun emitInterrupted() {
        onEvent?.invoke(GeminiLiveEvent.Interrupted(playbackGeneration))
    }

    fun emitTurnComplete() {
        onEvent?.invoke(GeminiLiveEvent.Content(emptyList(), playbackGeneration, false, true, false))
    }

    fun emitAudio(generation: Long?) {
        onEvent?.invoke(GeminiLiveEvent.Content(listOf(ByteArray(2)), generation, true, false, false))
    }
}
