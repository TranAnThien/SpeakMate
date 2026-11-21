package com.project.speakmate.ui.history.state

import androidx.compose.runtime.Immutable

@Immutable
data class SessionActionUiState(
    val id: String = "",
    val text: String = "",
    val isCompleted: Boolean = false
)
