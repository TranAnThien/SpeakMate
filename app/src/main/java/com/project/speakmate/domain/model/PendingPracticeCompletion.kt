package com.project.speakmate.domain.model

data class PendingPracticeCompletion(
    val attemptId: String,
    val practiceType: PracticeType?,
    val scenarioTitle: String,
    val startedAtEpochMillis: Long,
    val status: PracticeCompletionStatus,
    val saveFailure: PracticeSaveFailure? = null
)
