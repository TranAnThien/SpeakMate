package com.project.speakmate.ui.history.state

import androidx.compose.runtime.Immutable
import com.project.speakmate.domain.model.SenderType

@Immutable
data class SessionMessageUiState(
    val id: String = "",
    val metadata: String = "",
    val text: String = "",
    val isUser: Boolean = false,
    val sender: SenderType? = null,
    val isInterrupted: Boolean = false
)
