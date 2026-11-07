package com.project.speakmate.ui.call.state

import androidx.compose.runtime.Immutable

@Immutable
data class CallTranscriptMessageUiState(
    val id: String = "",
    val text: String = "",
    val isUser: Boolean = false
)
