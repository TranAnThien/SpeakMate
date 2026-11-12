package com.project.speakmate.domain.model

data class GoalAssessment(
    val scenarioId: String,
    val title: String,
    val requiredActions: List<RequiredAction>
)