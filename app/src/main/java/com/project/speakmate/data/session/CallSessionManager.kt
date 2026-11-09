package com.project.speakmate.data.session

import com.project.speakmate.data.remote.ai.GeminiLiveEvent
import com.project.speakmate.domain.model.CallFailure
import com.project.speakmate.domain.model.CallInterruption
import com.project.speakmate.domain.model.CallOutcome
import com.project.speakmate.domain.model.CallSessionState
import com.project.speakmate.domain.model.CallState
import com.project.speakmate.domain.model.PronunciationState
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.utils.extension.default
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield

class CallSessionManager(
    private val createMedia: () -> CallSessionMedia,
    dispatcher: CoroutineDispatcher,
    private val nowMillis: () -> Long
) {
    companion object {
        private const val SETUP_TIMEOUT_MILLIS = 15_000L
        private const val RESPONSE_TIMEOUT_MILLIS = 20_000L
        private const val PAUSE_TIMEOUT_MILLIS = 30_000L
        private const val RESUME_TURN_TIMEOUT_MILLIS = 4_000L
        private const val RECONNECT_WINDOW_MILLIS = 30_000L
        private const val MICROPHONE_WINDOW_MILLIS = 60_000L
        private const val FINALIZE_WINDOW_MILLIS = 3_000L
        private const val MAX_SERVER_EVENTS = 256
    }

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val isClosed = AtomicBoolean(false)
    private val mediaRevision = AtomicLong()
    private val uplinkRevision = AtomicLong()
    private val commands = Channel<() -> Unit>(Channel.UNLIMITED)
    private val reconnectPolicy = ReconnectPolicy()
    private val mutableSessionState = MutableStateFlow(CallSessionState())
    val sessionState = mutableSessionState.asStateFlow()
    private val activeAudioLevel = MutableStateFlow<StateFlow<Float>?>(null)
    @OptIn(ExperimentalCoroutinesApi::class)
    val audioLevel = activeAudioLevel.flatMapLatest { it ?: flowOf(0f) }
        .stateIn(scope, SharingStarted.Eagerly, 0f)
    private var isForeground = false
    private var session: CallSession? = null

    init {
        scope.launch {
            for (command in commands) command()
        }
    }

    fun startCall(scenario: Scenario) {
        val revision = mediaRevision.get()
        commands.trySend command@{
            if (!isMediaCurrent(revision) || !isForeground || session != null) return@command
            val media = try {
                createMedia()
            } catch (_: Exception) {
                mutableSessionState.value = CallSessionState(
                    state = CallState.FAILED,
                    failure = CallFailure.CONFIGURATION,
                    outcome = CallOutcome.FAILED
                )
                return@command
            }
            val current = CallSession(media, Channel(MAX_SERVER_EVENTS), createSessionScope(), nowMillis(), revision, scenario)
            session = current
            try {
                connect(current)
            } catch (_: Exception) {
                finish(current, CallFailure.CONFIGURATION)
            }
        }
    }

    fun toggleMute() {
        if (mutableSessionState.value.state != CallState.ACTIVE) return
        val attemptRevision = mediaRevision.get()
        val microphoneRevision = uplinkRevision.incrementAndGet()
        commands.trySend command@{
            val current = session ?: return@command
            val state = mutableSessionState.value
            if (state.state != CallState.ACTIVE || state.interruption != null || current.isFinalizing ||
                current.mediaRevision != attemptRevision || !canOpenAudio(current)
            ) return@command
            val isMuted = !state.isMuted
            runMediaOperation(current) {
                if (isMuted) {
                    current.media.suspendUplink()
                    current.media.finishAudioInput()
                } else {
                    if (!current.isCallAudioHeld) enableUplink(current, microphoneRevision)
                    current.media.getConnectionFailure()?.let { failure ->
                        handleTransportFailure(current, failure)
                        return@command
                    }
                }
                mutableSessionState.update { it.copy(isMuted = isMuted) }
            }
        }
    }

    fun setForeground(isForeground: Boolean) {
        if (!isForeground) revokeMedia()
        commands.trySend command@{
            this.isForeground = isForeground
            val current = session ?: return@command
            if (current.isFinalizing) return@command
            if (isForeground) {
                if (hasPauseExpired()) finish(current, CallFailure.PAUSE_EXPIRED)
                return@command
            }
            when (mutableSessionState.value.state) {
                CallState.CALLING -> finish(current)
                CallState.ACTIVE -> pause(current)
                CallState.PAUSED -> {
                    current.resumeJob?.cancel()
                    current.resumeJob = null
                    current.shouldResumeAfterReconnect = false
                    current.media.suspendMedia()
                    mutableSessionState.update { it.copy(isResuming = false) }
                }
                else -> Unit
            }
        }
    }

    fun resumeCall() {
        val revision = mediaRevision.get()
        commands.trySend command@{
            val current = session ?: return@command
            val state = mutableSessionState.value
            if (state.state != CallState.PAUSED || state.interruption == CallInterruption.NETWORK || current.isFinalizing ||
                !isForeground || !isMediaCurrent(revision) || current.resumeJob?.isActive == true
            ) return@command
            current.mediaRevision = revision
            val failure = when {
                hasPauseExpired() -> CallFailure.PAUSE_EXPIRED
                !current.media.hasMicrophonePermission() -> CallFailure.MICROPHONE_PERMISSION
                else -> current.media.getConnectionFailure()
            }
            if (failure != null) {
                finish(current, failure)
                return@command
            }
            mutableSessionState.update { it.copy(isResuming = true) }
            val generation = ++current.audioGeneration
            current.resumeJob = current.scope.launch { resumeAudio(current, generation) }
        }
    }

    fun playPronunciation(text: String) {
        commands.trySend command@{
            val current = session ?: return@command
            val state = mutableSessionState.value.state
            if (current.isFinalizing || text.isBlank() || state != CallState.ACTIVE && state != CallState.PAUSED) return@command
            val revision = ++current.pronunciationRevision
            if (!current.isPronouncing) holdCallAudio(current)
            current.isPronouncing = true
            mutableSessionState.update { it.copy(pronunciation = PronunciationState.PLAYING) }
            try {
                current.media.speakPronunciation(text) { isSuccess ->
                    commands.trySend {
                        if (session === current && current.pronunciationRevision == revision) endPronunciation(current, isSuccess)
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                endPronunciation(current, isSuccess = false)
            }
        }
    }

    fun stopPronunciation() {
        commands.trySend command@{
            val current = session ?: return@command
            if (!current.isPronouncing) {
                mutableSessionState.update {
                    if (it.pronunciation == PronunciationState.FAILED) it.copy(pronunciation = PronunciationState.IDLE) else it
                }
                return@command
            }
            cancelPronunciation(current)
            releaseCallAudio(current)
        }
    }

    fun endCall() {
        revokeMedia()
        commands.trySend { session?.let(::requestEnd) }
    }

    fun close() {
        if (!isClosed.compareAndSet(false, true)) return
        revokeMedia()
        commands.trySend {
            try {
                val current = session
                if (current != null) finish(current)
                else mutableSessionState.update { it.copy(isClosed = true) }
            } finally {
                commands.cancel()
                scope.cancel()
            }
        }
    }

    private fun createSessionScope(): CoroutineScope =
        CoroutineScope(scope.coroutineContext + SupervisorJob(scope.coroutineContext.job))

    private fun connect(current: CallSession) {
        if (!current.media.hasMicrophonePermission()) {
            finish(current, CallFailure.MICROPHONE_PERMISSION)
            return
        }
        mutableSessionState.value = CallSessionState(state = CallState.CALLING)
        current.scope.launch {
            for (event in current.events) handleEvent(current, event)
        }
        startTimeout(current, SETUP_TIMEOUT_MILLIS, CallFailure.TIMEOUT)
        val generation = ++current.connectionGeneration
        current.media.connect(
            current.scenario,
            onEvent = { event -> receiveEvent(current, generation, event) },
            onFailure = { failure -> receiveFailure(current, generation, failure) }
        )
    }

    private fun receiveEvent(current: CallSession, generation: Long, event: GeminiLiveEvent) {
        if (current.connectionGeneration != generation) return
        if (current.events.trySend(event).isFailure) {
            commands.trySend { finish(current, CallFailure.PLAYBACK_BACKLOG) }
        }
    }

    private fun receiveFailure(current: CallSession, generation: Long, failure: CallFailure) {
        commands.trySend {
            if (current.connectionGeneration == generation) handleTransportFailure(current, failure)
        }
    }

    private fun handleTransportFailure(current: CallSession, failure: CallFailure) {
        if (session !== current) return
        val state = mutableSessionState.value
        when {
            current.isFinalizing -> finish(current, isEndedByUser = true)
            failure == CallFailure.NETWORK && state.startedAtMillis != null && current.resumptionHandle != null &&
                (state.state == CallState.ACTIVE || state.state == CallState.PAUSED) -> scheduleReconnect(current)
            else -> finish(current, failure)
        }
    }

    private fun scheduleReconnect(current: CallSession) {
        val now = nowMillis()
        val state = mutableSessionState.value
        if (state.interruption != CallInterruption.NETWORK) {
            current.disconnectedAtMillis = now
            current.reconnectAttempt = 0
            current.shouldResumeAfterReconnect = state.state == CallState.ACTIVE
            current.microphoneJob?.cancel()
            if (state.state == CallState.ACTIVE) {
                pause(current)
                if (session !== current) return
                startTimeout(current, RECONNECT_WINDOW_MILLIS, CallFailure.NETWORK)
            }
            mutableSessionState.update { it.copy(interruption = CallInterruption.NETWORK, isResuming = false) }
        }
        val disconnectedAt = current.disconnectedAtMillis.default(now)
        val remainingMillis = RECONNECT_WINDOW_MILLIS - (now - disconnectedAt)
        if (remainingMillis <= 0L) {
            finish(current, CallFailure.NETWORK)
            return
        }
        val delayMillis = reconnectPolicy.getDelayMillis(current.reconnectAttempt++).coerceAtMost(remainingMillis)
        current.reconnectJob?.cancel()
        current.reconnectJob = current.scope.launch {
            delay(delayMillis)
            reconnect(current)
        }
    }

    private fun reconnect(current: CallSession) {
        val handle = current.resumptionHandle
        if (session !== current || mutableSessionState.value.interruption != CallInterruption.NETWORK || handle == null) return
        val generation = ++current.connectionGeneration
        try {
            current.media.reconnect(
                current.scenario,
                handle,
                onEvent = { event -> receiveEvent(current, generation, event) },
                onFailure = { failure -> receiveFailure(current, generation, failure) }
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            scheduleReconnect(current)
        }
    }

    private fun completeReconnect(current: CallSession) {
        current.reconnectJob?.cancel()
        current.reconnectJob = null
        current.timeoutJob?.cancel()
        current.disconnectedAtMillis = null
        current.reconnectAttempt = 0
        mutableSessionState.update { it.copy(interruption = null) }
        if (current.shouldResumeAfterReconnect && canOpenAudio(current)) {
            current.shouldResumeAfterReconnect = false
            openAudio(current)
            return
        }
        current.shouldResumeAfterReconnect = false
        val pausedAt = mutableSessionState.value.pausedAtMillis ?: return
        startTimeout(current, (PAUSE_TIMEOUT_MILLIS - (nowMillis() - pausedAt)).coerceAtLeast(0L), CallFailure.PAUSE_EXPIRED)
    }

    private fun requestEnd(current: CallSession) {
        if (current.isFinalizing) return
        val state = mutableSessionState.value
        val canFinalize = state.startedAtMillis != null && state.interruption != CallInterruption.NETWORK &&
            current.transcript.hasOpenUserTurn && current.media.getConnectionFailure() == null
        if (canFinalize) beginFinalize(current)
        else finish(current, isEndedByUser = true)
    }

    private fun beginFinalize(current: CallSession) {
        val now = nowMillis()
        current.isFinalizing = true
        current.endRequestedAtMillis = now
        current.timeoutJob?.cancel()
        current.resumeJob?.cancel()
        current.reconnectJob?.cancel()
        current.microphoneJob?.cancel()
        cancelPronunciation(current)
        current.speechEvidence.endSpeech(now)
        runMediaOperation(current) {
            current.media.suspendUplink()
            current.media.finishAudioInput()
            current.transcript.interruptAiTurn(current.media.clearPlayback().playedRatio)
            closeAudio(current)
        }
        if (session !== current) return
        publishSpeechEvidence(current)
        publishTranscript(current)
        mutableSessionState.update {
            it.copy(
                isFinalizing = true,
                endedAtMillis = now,
                isPlaying = false,
                isProcessing = false,
                isAiInterrupted = false,
                interruption = null
            )
        }
        current.timeoutJob = current.scope.launch {
            delay(FINALIZE_WINDOW_MILLIS)
            finish(current, isEndedByUser = true)
        }
    }

    private suspend fun resumeAudio(current: CallSession, generation: Long) {
        var downlinkFailure: CallFailure? = null
        val hasTurnBoundary = try {
            withTimeoutOrNull(RESUME_TURN_TIMEOUT_MILLIS) {
                do {
                    current.media.awaitTurnBoundary()
                    if (!canOpenAudio(current)) return@withTimeoutOrNull false
                    downlinkFailure = current.media.enableDownlink(generation, requiresIdleTurn = true)
                    if (downlinkFailure == CallFailure.TIMEOUT) yield()
                } while (downlinkFailure == CallFailure.TIMEOUT)
                true
            } == true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            if (canOpenAudio(current)) finish(current, CallFailure.NETWORK)
            return
        }
        if (!canOpenAudio(current)) return
        val failure = if (hasTurnBoundary) downlinkFailure else CallFailure.TIMEOUT
        if (failure != null) finish(current, failure)
        else openAudio(current, enabledDownlinkGeneration = generation)
    }

    private fun handleEvent(current: CallSession, event: GeminiLiveEvent) {
        val state = mutableSessionState.value.state
        val canHandleAudio = state == CallState.ACTIVE && canOpenAudio(current)
        when (event) {
            GeminiLiveEvent.SetupComplete -> when {
                state == CallState.CALLING -> openAudio(current)
                mutableSessionState.value.interruption == CallInterruption.NETWORK -> completeReconnect(current)
            }
            is GeminiLiveEvent.SpeechActivity -> {
                if (!canHandleAudio || event.playbackGeneration != current.audioGeneration) return
                if (event.isSpeaking) {
                    current.timeoutJob?.cancel()
                    current.speechEvidence.startSpeech(nowMillis())
                    current.transcript.startUserTurn()
                    publishTranscript(current)
                } else {
                    startTimeout(current, RESPONSE_TIMEOUT_MILLIS, CallFailure.TIMEOUT)
                    current.speechEvidence.endSpeech(nowMillis())
                }
                mutableSessionState.update {
                    it.copy(isProcessing = !event.isSpeaking, isAiInterrupted = it.isAiInterrupted && event.isSpeaking)
                }
                publishSpeechEvidence(current)
            }
            is GeminiLiveEvent.Content -> {
                if (canHandleAudio) handleContent(current, event)
            }
            is GeminiLiveEvent.InputTranscription -> {
                if (session === current && mutableSessionState.value.startedAtMillis != null) {
                    current.speechEvidence.recordUserTranscript(event.text)
                    publishSpeechEvidence(current)
                    current.transcript.appendUserText(event.text, event.isFinished)
                    publishTranscript(current)
                    if (current.isFinalizing && !current.transcript.hasOpenUserTurn) finish(current, isEndedByUser = true)
                }
            }
            is GeminiLiveEvent.OutputTranscription -> {
                if (!canHandleAudio || event.playbackGeneration != current.audioGeneration) return
                if (!current.transcript.hasOpenAiTurn) current.media.markPlaybackTurn()
                current.transcript.appendAiText(event.text, event.isFinished)
                publishTranscript(current)
            }
            is GeminiLiveEvent.Interrupted -> {
                if (!canHandleAudio || event.playbackGeneration != current.audioGeneration) return
                val progress = current.media.clearPlayback()
                current.transcript.interruptAiTurn(progress.playedRatio)
                publishTranscript(current)
                mutableSessionState.update { it.copy(isAiInterrupted = true, isPlaying = false) }
            }
            is GeminiLiveEvent.ResumptionUpdate -> {
                if (session === current && event.isResumable) current.resumptionHandle = event.handle
            }
            is GeminiLiveEvent.GenerationComplete,
            is GeminiLiveEvent.GoAway -> Unit
        }
    }

    private fun handleContent(current: CallSession, content: GeminiLiveEvent.Content) {
        if (content.playbackGeneration != current.audioGeneration) return
        if (content.hasModelTurn) {
            current.speechEvidence.recordModelTurn()
            publishSpeechEvidence(current)
            if (!current.transcript.hasOpenAiTurn) current.media.markPlaybackTurn()
            current.transcript.startAiTurn()
            mutableSessionState.update { if (it.isAiInterrupted) it.copy(isAiInterrupted = false) else it }
        }
        for (chunk in content.audio) {
            if (!current.media.enqueuePlayback(chunk)) {
                finish(current, CallFailure.PLAYBACK_BACKLOG)
                return
            }
        }
        if (content.isTurnFinished || content.isWaitingForInput) current.transcript.completeAiTurn()
        publishTranscript(current)
        if (content.isTurnFinished || content.isWaitingForInput) {
            current.timeoutJob?.cancel()
            mutableSessionState.update { it.copy(isProcessing = false) }
        } else if (content.hasModelTurn) {
            startTimeout(current, RESPONSE_TIMEOUT_MILLIS, CallFailure.TIMEOUT)
            mutableSessionState.update { it.copy(isProcessing = content.audio.isEmpty() && !it.isPlaying) }
        }
    }

    private fun openAudio(current: CallSession, enabledDownlinkGeneration: Long? = null) {
        if (!canOpenAudio(current)) return
        if (!current.media.hasMicrophonePermission()) {
            finish(current, CallFailure.MICROPHONE_PERMISSION)
            return
        }
        val audioGeneration = enabledDownlinkGeneration ?: ++current.audioGeneration
        val revision = current.mediaRevision
        val isMediaCurrent = { isMediaCurrent(revision) }
        val isResuming = mutableSessionState.value.state == CallState.PAUSED
        runMediaOperation(current) {
            val audioLevel = current.media.openAudio(
                onFailure = { failure ->
                    commands.trySend {
                        if (canOpenAudio(current) && audioGeneration == current.audioGeneration) {
                            finish(current, if (current.media.hasMicrophonePermission()) failure else CallFailure.MICROPHONE_PERMISSION)
                        }
                    }
                },
                onInterrupted = {
                    commands.trySend {
                        if (canOpenAudio(current) && audioGeneration == current.audioGeneration &&
                            mutableSessionState.value.state == CallState.ACTIVE && !current.isFinalizing
                        ) {
                            revokeMedia()
                            pause(current)
                        }
                    }
                },
                onMicrophoneSilenced = { isSilenced ->
                    commands.trySend {
                        if (canOpenAudio(current) && audioGeneration == current.audioGeneration) {
                            setMicrophoneSilenced(current, isSilenced)
                        }
                    }
                }
            )
            if (!canOpenAudio(current)) {
                closeAudio(current)
                return
            }
            val downlinkFailure = if (enabledDownlinkGeneration != null) current.media.getConnectionFailure()
                else current.media.enableDownlink(audioGeneration, requiresIdleTurn = false)
            if (downlinkFailure != null) {
                finish(current, downlinkFailure)
                return
            }
            if (!canOpenAudio(current)) {
                closeAudio(current)
                return
            }
            current.media.startPlayback(current.scope, isMediaCurrent) { isPlaying ->
                commands.trySend {
                    if (canOpenAudio(current) && audioGeneration == current.audioGeneration) {
                        mutableSessionState.update { it.copy(isPlaying = isPlaying) }
                    }
                }
            }
            if (!mutableSessionState.value.isMuted) enableUplink(current, uplinkRevision.get())
            val activeAt = nowMillis()
            val activationFailure = when {
                isResuming && hasPauseExpired() -> CallFailure.PAUSE_EXPIRED
                !isResuming && activeAt - current.callingAtMillis >= SETUP_TIMEOUT_MILLIS -> CallFailure.TIMEOUT
                else -> current.media.getConnectionFailure()
            }
            if (!canOpenAudio(current)) {
                closeAudio(current)
                return
            }
            if (activationFailure != null) {
                finish(current, activationFailure)
                return
            }
            activate(current, audioLevel, activeAt)
        }
    }

    private fun setMicrophoneSilenced(current: CallSession, isSilenced: Boolean) {
        val state = mutableSessionState.value
        if (state.state != CallState.ACTIVE || current.isFinalizing) return
        if (isSilenced) {
            if (state.interruption == CallInterruption.MICROPHONE) return
            current.speechEvidence.endSpeech(nowMillis())
            publishSpeechEvidence(current)
            runMediaOperation(current) {
                current.media.blockUplink()
                current.media.finishAudioInput()
            }
            if (session !== current) return
            mutableSessionState.update { it.copy(interruption = CallInterruption.MICROPHONE) }
            current.microphoneJob?.cancel()
            current.microphoneJob = current.scope.launch {
                delay(MICROPHONE_WINDOW_MILLIS)
                finish(current, CallFailure.MICROPHONE_PERMISSION)
            }
            return
        }
        if (state.interruption != CallInterruption.MICROPHONE) return
        current.microphoneJob?.cancel()
        current.microphoneJob = null
        mutableSessionState.update { it.copy(interruption = null) }
        if (!state.isMuted && !current.isCallAudioHeld) runMediaOperation(current) { enableUplink(current, uplinkRevision.get()) }
    }

    private fun holdCallAudio(current: CallSession) {
        if (mutableSessionState.value.state != CallState.ACTIVE || !canOpenAudio(current)) return
        current.isCallAudioHeld = true
        runSupportOperation {
            current.media.blockUplink()
            current.media.finishAudioInput()
            current.media.setPlaybackPaused(true)
        }
    }

    private fun releaseCallAudio(current: CallSession) {
        if (!current.isCallAudioHeld) return
        current.isCallAudioHeld = false
        val state = mutableSessionState.value
        if (state.state != CallState.ACTIVE || current.isFinalizing || !canOpenAudio(current)) return
        runSupportOperation {
            current.media.setPlaybackPaused(false)
            if (!state.isMuted && state.interruption == null) enableUplink(current, uplinkRevision.get())
        }
    }

    private fun endPronunciation(current: CallSession, isSuccess: Boolean) {
        current.isPronouncing = false
        releaseCallAudio(current)
        mutableSessionState.update {
            it.copy(pronunciation = if (isSuccess) PronunciationState.IDLE else PronunciationState.FAILED)
        }
    }

    private fun cancelPronunciation(current: CallSession) {
        if (!current.isPronouncing && !current.isCallAudioHeld) return
        current.pronunciationRevision++
        current.isPronouncing = false
        runSupportOperation { current.media.stopPronunciation() }
        mutableSessionState.update { it.copy(pronunciation = PronunciationState.IDLE) }
    }

    private inline fun runSupportOperation(operation: () -> Unit) {
        try {
            operation()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            Unit
        }
    }

    private fun activate(current: CallSession, audioLevel: StateFlow<Float>, activeAt: Long) {
        current.timeoutJob?.cancel()
        activeAudioLevel.value = audioLevel
        mutableSessionState.update {
            val pauseMillis = it.pausedAtMillis?.let { pausedAt -> activeAt - pausedAt }.default(0L)
            it.copy(
                state = CallState.ACTIVE,
                startedAtMillis = it.startedAtMillis.default(activeAt),
                pausedAtMillis = null,
                totalPausedMillis = it.totalPausedMillis + pauseMillis,
                isResuming = false,
                isProcessing = false
            )
        }
    }

    private fun pause(current: CallSession) {
        val now = nowMillis()
        current.microphoneJob?.cancel()
        current.microphoneJob = null
        cancelPronunciation(current)
        current.isCallAudioHeld = false
        current.speechEvidence.endSpeech(now)
        publishSpeechEvidence(current)
        current.transcript.interruptOpenTurns(current.media.clearPlayback().playedRatio)
        publishTranscript(current)
        runMediaOperation(current) {
            current.media.suspendMedia()
            current.media.finishAudioInput()
            closeAudio(current)
            mutableSessionState.update {
                it.copy(
                    state = CallState.PAUSED,
                    pausedAtMillis = now,
                    isPlaying = false,
                    isProcessing = false,
                    isResuming = false,
                    isAiInterrupted = false,
                    interruption = null
                )
            }
            startTimeout(current, PAUSE_TIMEOUT_MILLIS, CallFailure.PAUSE_EXPIRED)
        }
    }

    private inline fun runMediaOperation(current: CallSession, operation: () -> Unit) {
        try {
            operation()
        } catch (cancellation: CancellationException) {
            finish(current)
            throw cancellation
        } catch (_: SecurityException) {
            finish(current, CallFailure.MICROPHONE_PERMISSION)
        } catch (_: Exception) {
            finish(current, CallFailure.AUDIO)
        }
    }

    private fun canOpenAudio(current: CallSession): Boolean =
        session === current && isForeground && isMediaCurrent(current.mediaRevision)

    private fun enableUplink(current: CallSession, revision: Long) {
        val attemptRevision = current.mediaRevision
        current.media.enableUplink(current.scope) {
            isMediaCurrent(attemptRevision) && uplinkRevision.get() == revision
        }
    }

    private fun isMediaCurrent(revision: Long): Boolean =
        !isClosed.get() && mediaRevision.get() == revision

    private fun revokeMedia() {
        mediaRevision.incrementAndGet()
    }

    private fun hasPauseExpired(): Boolean {
        val pausedAt = mutableSessionState.value.pausedAtMillis ?: return false
        return nowMillis() - pausedAt >= PAUSE_TIMEOUT_MILLIS
    }

    private fun startTimeout(current: CallSession, delayMillis: Long, failure: CallFailure) {
        current.timeoutJob?.cancel()
        current.timeoutJob = current.scope.launch {
            delay(delayMillis)
            val isUnconditional = failure == CallFailure.PAUSE_EXPIRED || failure == CallFailure.NETWORK
            if (isUnconditional || canOpenAudio(current)) finish(current, failure)
        }
    }

    private fun publishSpeechEvidence(current: CallSession) {
        val evidence = current.speechEvidence
        mutableSessionState.update {
            if (it.isUserSpeaking == evidence.isUserSpeaking && it.hasUserSpeech == evidence.hasUserSpeech) it
            else it.copy(isUserSpeaking = evidence.isUserSpeaking, hasUserSpeech = evidence.hasUserSpeech)
        }
    }

    private fun publishTranscript(current: CallSession) {
        val transcript = current.transcript.snapshot()
        mutableSessionState.update { if (it.transcript == transcript) it else it.copy(transcript = transcript) }
    }

    private fun finish(current: CallSession, failure: CallFailure? = null, isEndedByUser: Boolean = false) {
        if (session !== current) return
        val now = nowMillis()
        val endedAt = current.endRequestedAtMillis.default(now)
        val isUserEnd = isEndedByUser || current.isFinalizing
        current.speechEvidence.endSpeech(endedAt)
        val playedRatio = try {
            current.media.clearPlayback().playedRatio
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            0f
        }
        current.transcript.interruptOpenTurns(playedRatio)
        publishTranscript(current)
        cancelPronunciation(current)
        current.isCallAudioHeld = false
        mediaRevision.compareAndSet(current.mediaRevision, current.mediaRevision + 1)
        session = null
        current.scope.cancel()
        current.events.cancel()
        current.audioGeneration++
        current.connectionGeneration++
        activeAudioLevel.value = null
        val terminalFailure = try {
            current.media.close()
            failure
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            failure.default(CallFailure.AUDIO)
        }
        mutableSessionState.update {
            it.copy(
                state = if (terminalFailure == null) CallState.ENDED else CallState.FAILED,
                failure = terminalFailure,
                outcome = when {
                    terminalFailure != null -> CallOutcome.FAILED
                    isUserEnd && it.startedAtMillis != null && current.speechEvidence.hasUserSpeech -> CallOutcome.COMPLETED
                    else -> CallOutcome.CANCELLED
                },
                isUserSpeaking = false,
                isAiInterrupted = false,
                hasUserSpeech = current.speechEvidence.hasUserSpeech,
                endedAtMillis = endedAt,
                totalPausedMillis = it.totalPausedMillis +
                    it.pausedAtMillis?.let { pausedAt -> endedAt - pausedAt }.default(0L).coerceAtLeast(0L),
                pausedAtMillis = null,
                interruption = null,
                isFinalizing = false,
                pronunciation = PronunciationState.IDLE,
                isMuted = false,
                isPlaying = false,
                isProcessing = false,
                isResuming = false,
                isClosed = isClosed.get()
            )
        }
    }

    private fun closeAudio(current: CallSession) {
        current.audioGeneration++
        activeAudioLevel.value = null
        current.media.closeAudio()
    }
}
