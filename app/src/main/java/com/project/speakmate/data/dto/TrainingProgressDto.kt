package com.project.speakmate.data.dto

import com.project.speakmate.domain.model.TrainingProgress
import com.project.speakmate.domain.model.TrainingStage

data class TrainingProgressDto(
    val currentGoalOrder: Int = 0,
    val currentUnitOrder: Int = 0,
    val stage: String = TrainingStage.UNIT.name
)

fun TrainingProgress.toDto() = TrainingProgressDto(currentGoalOrder, currentUnitOrder, stage.name)
