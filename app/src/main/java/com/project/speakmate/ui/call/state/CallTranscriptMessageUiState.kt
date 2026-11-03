package com.project.speakmate.ui.call.state

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class CallTranscriptMessageUiState(
    val id: String = "",
    val text: String = "",
    val isUser: Boolean = false
)
