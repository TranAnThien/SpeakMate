package com.project.speakmate.data.dto

import com.google.firebase.Timestamp
import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.PracticeAttempt
import com.project.speakmate.domain.model.PracticeType
import com.project.speakmate.domain.model.TranscriptMessage
import com.project.speakmate.utils.extension.default

data class PracticeAttemptDto(
    val attemptId: String = "",
    val scenarioId: String = "",
    val practiceType: String = "",
    val startedAt: Timestamp? = null,
    val endedAt: Timestamp? = null,
    val result: String? = null,
    val criteriaResult: SpeakingCriteriaResultDto? = null,
    val requiredActionResults: Map<String, Boolean> = emptyMap(),
    val feedbackSummary: String? = null,
    val requiredActionDescriptions: Map<String, String> = emptyMap()
)

fun PracticeAttempt.toDto(): PracticeAttemptDto {
    return PracticeAttemptDto(
        attemptId = this.attemptId,
        scenarioId = this.scenarioId,
        practiceType = this.practiceType.name,
        startedAt = Timestamp(this.startedAt),
        endedAt = Timestamp(this.endedAt),
        result = this.result?.name,
        criteriaResult = criteriaResult?.toDto(),
        requiredActionResults = this.requiredActionResults,
        feedbackSummary = this.feedbackSummary,
        requiredActionDescriptions = this.requiredActionDescriptions
    )
}

fun PracticeAttemptDto.toDomain(transcripts: List<TranscriptMessage>): PracticeAttempt? {
    val type = PracticeType.entries.firstOrNull { it.name == practiceType } ?: return null
    val assessment =
        result?.let { storedResult -> AssessmentResult.entries.firstOrNull { it.name == storedResult } ?: return null }
    val started = this.startedAt?.toDate() ?: return null
    val ended = this.endedAt?.toDate().default(started)
    val criteria = criteriaResult?.let { it.toDomain() ?: return null }
    return PracticeAttempt(
        attemptId = this.attemptId,
        scenarioId = this.scenarioId,
        practiceType = type,
        startedAt = started,
        endedAt = ended,
        result = assessment,
        criteriaResult = criteria,
        requiredActionResults = this.requiredActionResults,
        transcripts = transcripts,
        feedbackSummary = this.feedbackSummary,
        requiredActionDescriptions = this.requiredActionDescriptions
    )
}
