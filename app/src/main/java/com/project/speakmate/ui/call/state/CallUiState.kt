package com.project.speakmate.ui.call.state

import com.project.speakmate.domain.model.CallFailure
import com.project.speakmate.domain.model.CallSessionState

data class CallUiState(
    val presentation: CallPresentation = CallPresentation.READY,
    val title: String = "",
    val isEndConfirmationVisible: Boolean = false,
    val isTranscriptVisible: Boolean = false,
    val messages: List<CallTranscriptMessageUiState> = emptyList(),
    val dictionary: CallDictionaryUiState? = null,
    val isTranscriptAvailable: Boolean = true,
    val isMuted: Boolean = false,
    val isResuming: Boolean = false,
    val microphonePermission: MicrophonePermissionUiState? = null,
    val isExiting: Boolean = false,
    val isExitReady: Boolean = false,
    val isStandaloneCall: Boolean = false,
    val failure: CallFailure? = null,
    val finishedSession: CallSessionState? = null
)
