package com.project.speakmate.domain.model

data class PlacementTaskRecord(
    val step: PlacementStep,
    val taskId: String,
    val scenarioId: String,
    val result: AssessmentResult,
    val criteria: SpeakingCriteriaResult
)
