package com.project.speakmate.domain.model

sealed interface AssessmentOutcome {
    data class Evaluated(
        val result: AssessmentResult,
        val criteria: SpeakingCriteriaResult,
        val requiredActionResults: Map<String, Boolean>,
        val evaluation: SpeakingEvaluation
    ) : AssessmentOutcome

    data class InsufficientEvidence(val reason: InsufficientEvidenceReason) : AssessmentOutcome
}
