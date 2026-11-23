package com.project.speakmate.domain.model



data class AssessmentGradingState(
    val outcome: AssessmentOutcome? = null,
    val isGrading: Boolean = false,
    val canRetry: Boolean = false,
    val isExpired: Boolean = false
)
