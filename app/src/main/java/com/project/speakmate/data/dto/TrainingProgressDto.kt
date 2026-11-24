package com.project.speakmate.data.dto

import com.project.speakmate.domain.model.TrainingProgress

data class TrainingProgressDto(
    val currentGoalOrder: Int? = null,
    val currentUnitOrder: Int? = null,
    val stage: String? = null
)

fun TrainingProgress.toDto() =
    TrainingProgressDto(
        currentGoalOrder = currentGoalOrder,
        currentUnitOrder = currentUnitOrder,
        stage = stage.name
    )
