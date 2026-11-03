package com.project.speakmate.ui.history.state

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class SessionMessageUiState(
    val id: String = "",
    val metadata: String = "",
    val text: String = "",
    val isUser: Boolean = false
)
