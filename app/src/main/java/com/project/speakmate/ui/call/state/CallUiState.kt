package com.project.speakmate.ui.call.state

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class CallUiState(
    val presentation: CallPresentation = CallPresentation.READY,
    val title: String = "",
    val durationLabel: String = "",
    val isEndConfirmationVisible: Boolean = false,
    val isTranscriptVisible: Boolean = false,
    val messages: List<CallTranscriptMessageUiState> = emptyList(),
    val dictionary: CallDictionaryUiState? = null,
    val isTranscriptAvailable: Boolean = true
)
