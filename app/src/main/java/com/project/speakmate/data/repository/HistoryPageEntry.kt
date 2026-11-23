package com.project.speakmate.data.repository

import com.project.speakmate.domain.model.PracticeAttempt

data class HistoryPageEntry(
    val attemptId: String,
    val startedAtSeconds: Long?,
    val startedAtNanoseconds: Int?,
    val attempt: PracticeAttempt?,
    val hasPendingWrites: Boolean
)
