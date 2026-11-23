package com.project.speakmate.domain.model

data class PracticeHistoryCursor(
    val userId: String,
    val practiceType: PracticeType?,
    val startedAtSeconds: Long,
    val startedAtNanoseconds: Int,
    val attemptId: String
)
