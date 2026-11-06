package com.project.speakmate.data.session

import com.project.speakmate.data.remote.ai.GeminiLiveEvent
import com.project.speakmate.domain.model.CallFailure
import com.project.speakmate.domain.model.CallSessionState
import com.project.speakmate.domain.model.CallState
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
        private const val MAX_SERVER_EVENTS = 32
    }

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val isClosed = AtomicBoolean(false)
    private val mediaRevision = AtomicLong()
    private val uplinkRevision = AtomicLong()
    private val commands = Channel<() -> Unit>(Channel.UNLIMITED)
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
                mutableSessionState.value = CallSessionState(state = CallState.FAILED, failure = CallFailure.CONFIGURATION)
                return@command
            }
            val current = CallSession(media, Channel(MAX_SERVER_EVENTS), createSessionScope(), nowMillis(), revision)
            session = current
            try {
                connect(current, scenario)
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
            if (state.state != CallState.ACTIVE || current.mediaRevision != attemptRevision || !canOpenAudio(current)) {
                return@command
            }
            val isMuted = !state.isMuted
            runMediaOperation(current) {
                if (isMuted) {
                    current.media.suspendUplink()
                    current.media.finishAudioInput()
                } else {
                    enableUplink(current, microphoneRevision)
                    current.media.getConnectionFailure()?.let { failure ->
                        finish(current, failure)
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
            if (mutableSessionState.value.state != CallState.PAUSED ||
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

    fun endCall() {
        revokeMedia()
        commands.trySend { session?.let { finish(it) } }
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

    private fun connect(current: CallSession, scenario: Scenario) {
        if (!current.media.hasMicrophonePermission()) {
            finish(current, CallFailure.MICROPHONE_PERMISSION)
            return
        }
        mutableSessionState.value = CallSessionState(state = CallState.CALLING)
        current.scope.launch {
            for (event in current.events) handleEvent(current, event)
        }
        startTimeout(current, SETUP_TIMEOUT_MILLIS, CallFailure.TIMEOUT)
        current.media.connect(
            scenario,
            onEvent = { event -> receiveEvent(current, event) },
            onFailure = { failure -> commands.trySend { finish(current, failure) } }
        )
    }

    private fun receiveEvent(current: CallSession, event: GeminiLiveEvent) {
        if (current.events.trySend(event).isFailure) {
            commands.trySend { finish(current, CallFailure.PLAYBACK_BACKLOG) }
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
            GeminiLiveEvent.SetupComplete -> {
                if (state == CallState.CALLING) openAudio(current)
            }
            is GeminiLiveEvent.SpeechActivity -> {
                if (!canHandleAudio || event.playbackGeneration != current.audioGeneration) return
                if (event.isSpeaking) current.timeoutJob?.cancel()
                else startTimeout(current, RESPONSE_TIMEOUT_MILLIS, CallFailure.TIMEOUT)
                mutableSessionState.update { it.copy(isProcessing = !event.isSpeaking) }
            }
            is GeminiLiveEvent.Content -> {
                if (canHandleAudio) handleContent(current, event)
            }
        }
    }

    private fun handleContent(current: CallSession, content: GeminiLiveEvent.Content) {
        if (content.playbackGeneration != current.audioGeneration) return
        for (chunk in content.audio) {
            if (!current.media.enqueuePlayback(chunk)) {
                finish(current, CallFailure.PLAYBACK_BACKLOG)
                return
            }
        }
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
                            mutableSessionState.value.state == CallState.ACTIVE
                        ) {
                            revokeMedia()
                            pause(current)
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
        runMediaOperation(current) {
            current.media.suspendMedia()
            current.media.finishAudioInput()
            closeAudio(current)
            mutableSessionState.update {
                it.copy(state = CallState.PAUSED, pausedAtMillis = now, isPlaying = false, isProcessing = false, isResuming = false)
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
            if (failure == CallFailure.PAUSE_EXPIRED || canOpenAudio(current)) finish(current, failure)
        }
    }

    private fun finish(current: CallSession, failure: CallFailure? = null) {
        if (session !== current) return
        val now = nowMillis()
        mediaRevision.compareAndSet(current.mediaRevision, current.mediaRevision + 1)
        session = null
        current.scope.cancel()
        current.events.cancel()
        current.audioGeneration++
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
                endedAtMillis = now,
                totalPausedMillis = it.totalPausedMillis +
                    it.pausedAtMillis?.let { pausedAt -> now - pausedAt }.default(0L),
                pausedAtMillis = null,
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
