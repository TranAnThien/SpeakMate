package com.project.speakmate.ui.call

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.model.CallSessionState
import com.project.speakmate.domain.model.CallState
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.repository.CallRepository
import com.project.speakmate.ui.call.state.CallPresentation
import com.project.speakmate.ui.call.state.CallUiState
import com.project.speakmate.ui.call.state.MicrophonePermissionPresentation
import com.project.speakmate.ui.call.state.MicrophonePermissionUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class CallViewModel @Inject constructor(private val repository: CallRepository) : ViewModel() {
    companion object {
        private const val MILLIS_PER_SECOND = 1_000L
        private const val SECONDS_PER_MINUTE = 60L
        private const val DURATION_FORMAT = "%02d:%02d"
    }

    private val _uiState = MutableStateFlow(
        CallUiState(
            isTranscriptAvailable = false,
            isStandaloneCall = true
        )
    )
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()
    val audioLevel: StateFlow<Float> = repository.audioLevel
    val durationLabel: StateFlow<String> = combine(repository.sessionState, flow {
        while (true) {
            emit(Unit)
            delay(MILLIS_PER_SECOND)
        }
    }) { session, _ ->
        formatDuration(session)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")

    init {
        viewModelScope.launch {
            repository.sessionState.collect { session ->
                _uiState.update {
                    it.copy(
                        presentation = getPresentation(session),
                        isMuted = session.isMuted,
                        isResuming = session.isResuming,
                        failure = session.failure,
                        isEndConfirmationVisible = it.isEndConfirmationVisible &&
                            (session.state == CallState.ACTIVE ||
                                session.state == CallState.PAUSED && it.presentation == CallPresentation.PAUSED),
                        isExitReady = it.isExiting && session.isClosed
                    )
                }
            }
        }
    }

    override fun onCleared() {
        repository.close()
        super.onCleared()
    }

    fun setForeground(isForeground: Boolean) {
        repository.setForeground(isForeground)
        if (!isForeground) dismissEndConfirmation()
    }

    fun startCall(scenario: Scenario) {
        dismissMicrophonePermission()
        repository.startCall(scenario)
    }

    fun showMicrophonePermission(presentation: MicrophonePermissionPresentation) {
        _uiState.update { it.copy(microphonePermission = MicrophonePermissionUiState(presentation)) }
    }

    fun dismissMicrophonePermission() {
        _uiState.update { it.copy(microphonePermission = null) }
    }

    fun toggleMute() {
        repository.toggleMute()
    }

    fun requestEndCall() {
        when (repository.sessionState.value.state) {
            CallState.CALLING -> repository.endCall()
            CallState.ACTIVE, CallState.PAUSED -> _uiState.update { it.copy(isEndConfirmationVisible = true) }
            else -> Unit
        }
    }

    fun confirmEndCall() {
        dismissEndConfirmation()
        repository.endCall()
    }

    fun dismissEndConfirmation() {
        _uiState.update { it.copy(isEndConfirmationVisible = false) }
    }

    fun resumeCall() {
        repository.resumeCall()
    }

    fun exitCall() {
        if (_uiState.value.isExiting) return
        _uiState.update {
            it.copy(
                microphonePermission = null,
                isExiting = true,
                isExitReady = repository.sessionState.value.isClosed
            )
        }
        repository.close()
    }

    private fun getPresentation(session: CallSessionState): CallPresentation = when (session.state) {
        CallState.READY, CallState.ENDED -> CallPresentation.READY
        CallState.CALLING -> CallPresentation.CONNECTING
        CallState.PAUSED -> CallPresentation.PAUSED
        CallState.FAILED -> CallPresentation.FAILED
        CallState.ACTIVE -> when {
            session.isPlaying -> CallPresentation.SPEAKING
            session.isMuted -> CallPresentation.MUTED
            session.isProcessing -> CallPresentation.PROCESSING
            else -> CallPresentation.LISTENING
        }
    }

    private fun formatDuration(session: CallSessionState): String {
        val startedAt = session.startedAtMillis ?: return ""
        val until = session.endedAtMillis ?: session.pausedAtMillis ?: SystemClock.elapsedRealtime()
        val seconds = (until - startedAt - session.totalPausedMillis).coerceAtLeast(0L) / MILLIS_PER_SECOND
        return String.format(Locale.ROOT, DURATION_FORMAT, seconds / SECONDS_PER_MINUTE, seconds % SECONDS_PER_MINUTE)
    }
}
