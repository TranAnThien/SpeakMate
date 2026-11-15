package com.project.speakmate.domain.model

data class ScenarioVariant(
    val variantId: String,
    val scenarioId: String,
    val activity: TrainingActivity,
    val promptTemplate: String,
    val targetLevel: SpeakingLevel,
    val communicativeGoal: String,
    val requiredActionIds: Set<String>,
    val difficultyCode: String,
    val passConditionCode: String
)
