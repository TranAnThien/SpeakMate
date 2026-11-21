package com.project.speakmate.ui.call.state

data class CallRecoveryUiState(
    val userId: String? = null,
    val isLoading: Boolean = true,
    val wasInterrupted: Boolean = false,
    val isNoticeVisible: Boolean = false
)
