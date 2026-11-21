package com.project.speakmate.domain.model

data class PracticeCompletion(
    val attempt: PracticeAttempt,
    val training: TrainingActivityScenario? = null,
    val outcome: AssessmentOutcome? = null
)
