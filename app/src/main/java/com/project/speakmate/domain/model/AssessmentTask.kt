package com.project.speakmate.domain.model

data class AssessmentTask(
    val taskId: String,
    val scenarioId: String,
    val title: String,
    val targetLevel: SpeakingLevel,
    val communicativeGoal: String
)
