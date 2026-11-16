package com.project.speakmate.domain.model

import java.util.Date
import java.util.concurrent.TimeUnit

data class PracticeAttempt(
    val attemptId: String,
    val scenarioId: String,
    val practiceType: PracticeType,
    val startedAt: Date,
    val endedAt: Date,
    val result: AssessmentResult?,
    val criteriaResult: SpeakingCriteriaResult?,
    val requiredActionResults: Map<String, Boolean>,
    val transcripts: List<TranscriptMessage>,
    val feedbackSummary: String?
)

val PracticeAttempt.durationInSeconds: Long
    get() {
        val diffInMillis = endedAt.time - startedAt.time
        return TimeUnit.MILLISECONDS.toSeconds(diffInMillis).coerceAtLeast(0)
    }
