package com.project.speakmate.ui.call.state

import com.project.speakmate.domain.model.CallActivityType
import com.project.speakmate.domain.model.CallFailure
import com.project.speakmate.domain.model.CallSessionState

data class CallUiState(
    val presentation: CallPresentation = CallPresentation.READY,
    val isStartReady: Boolean = true,
    val activeDialog: CallDialog? = null,
    val isTranscriptVisible: Boolean = false,
    val messages: List<CallTranscriptMessageUiState> = emptyList(),
    val dictionary: CallDictionaryUiState? = null,
    val isTranscriptAvailable: Boolean = true,
    val isMuted: Boolean = false,
    val isResuming: Boolean = false,
    val microphonePermission: MicrophonePermissionUiState? = null,
    val isExiting: Boolean = false,
    val isExitReady: Boolean = false,
    val failure: CallFailure? = null,
    val finishedSession: CallSessionState? = null,
    val completionAttemptId: String? = null,
    val hasUserSpeech: Boolean = false,
    val hasCompletionError: Boolean = false,
    val activityType: CallActivityType = CallActivityType.FREE_PRACTICE,
    val activityIndex: Int? = null,
    val isFinishedSessionReady: Boolean = false,
    val hasConsumedExit: Boolean = false,
    val hasHandedOffCompletion: Boolean = false
) {
    val isTitleVisible: Boolean
        get() = activeDialog == CallDialog.Title

    val isEndConfirmationVisible: Boolean
        get() = activeDialog == CallDialog.EndConfirmation
}
