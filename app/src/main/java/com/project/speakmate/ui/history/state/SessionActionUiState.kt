package com.project.speakmate.ui.history.state

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class SessionActionUiState(
    val id: String = "",
    val text: String = "",
    val isCompleted: Boolean = false
)
