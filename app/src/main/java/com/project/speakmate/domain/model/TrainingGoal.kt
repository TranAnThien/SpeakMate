package com.project.speakmate.domain.model

data class TrainingGoal(
    val goalId: String,
    val title: String,
    val canDoDescription: String,
    val goalAssessment: GoalAssessment,
    val units: List<TrainingUnit>
)
