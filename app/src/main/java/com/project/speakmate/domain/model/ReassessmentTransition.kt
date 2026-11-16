package com.project.speakmate.domain.model

sealed interface ReassessmentTransition {
    data class NextStep(val state: ReassessmentState) : ReassessmentTransition

    data class RetryTask(
        val state: ReassessmentState,
        val reason: InsufficientEvidenceReason
    ) : ReassessmentTransition

    data class Promoted(
        val newLevel: SpeakingLevel,
        val latestAssessmentCriteria: SpeakingCriteriaResult,
        val lastAssessedAtMillis: Long,
        val newProgress: TrainingProgress = TrainingProgress(0, 0, TrainingStage.UNIT)
    ) : ReassessmentTransition

    data class NotPromoted(val step: ReassessmentStep, val reason: AssessmentResult) : ReassessmentTransition

    data class Stopped(val reason: StopReason) : ReassessmentTransition

    data class NotEligible(val reason: EligibilityReason) : ReassessmentTransition

    enum class StopReason {
        NO_TASK_AVAILABLE,
        TECHNICAL_RETRY_LIMIT,
        INVALID_STATE,
        UNEXPECTED_TASK
    }

    enum class EligibilityReason {
        LEVEL_NOT_ASSESSED,
        HIGHEST_LEVEL,
        PATH_NOT_COMPLETED
    }
}
