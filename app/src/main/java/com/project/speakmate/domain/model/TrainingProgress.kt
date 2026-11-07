package com.project.speakmate.domain.model

data class TrainingProgress(
    val currentGoalOrder: Int,
    val currentUnitOrder: Int,
    val stage: TrainingStage
)