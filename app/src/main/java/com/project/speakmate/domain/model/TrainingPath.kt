package com.project.speakmate.domain.model

data class TrainingPath(
    val pathId: String,
    val targetLevel: SpeakingLevel,
    val title: String,
    val goals: List<TrainingGoal>
)
