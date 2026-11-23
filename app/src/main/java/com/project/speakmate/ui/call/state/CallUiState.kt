package com.project.speakmate.ui.call.state

import com.project.speakmate.domain.model.CallActivityType
import com.project.speakmate.domain.model.CallFailure
import com.project.speakmate.domain.model.CallSessionState
import com.project.speakmate.domain.model.PlacementRole
import com.project.speakmate.domain.model.SpeakingLevel

data class CallUiState(
    val presentation: CallPresentation = CallPresentation.READY,
    val isStartReady: Boolean = true,
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
    val finishedSession: CallSessionState? = null,
    val completionAttemptId: String? = null,
    val hasUserSpeech: Boolean = false,
    val hasCompletionError: Boolean = false,
    val activityType: CallActivityType = CallActivityType.FREE_PRACTICE,
    val activityIndex: Int? = null,
    val isTitleVisible: Boolean = false,
    val activityLevel: SpeakingLevel? = null,
    val activityRole: PlacementRole? = null
)
