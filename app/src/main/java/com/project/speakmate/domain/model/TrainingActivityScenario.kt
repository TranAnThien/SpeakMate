package com.project.speakmate.domain.model

data class TrainingActivityScenario(
    val scenario: Scenario,
    val activity: TrainingActivity,
    val targetLevel: SpeakingLevel,
    val communicativeGoal: String,
    val requiredActions: List<RequiredAction>,
    val difficultyCode: String,
    val passConditionCode: String
)
