package com.project.speakmate.domain.model

data class CallSessionState(
    val state: CallState = CallState.READY,
    val isMuted: Boolean = false,
    val isPlaying: Boolean = false,
    val isProcessing: Boolean = false,
    val isResuming: Boolean = false,
    val startedAtMillis: Long? = null,
    val pausedAtMillis: Long? = null,
    val endedAtMillis: Long? = null,
    val failure: CallFailure? = null,
    val totalPausedMillis: Long = 0L,
    val isClosed: Boolean = false
)
