package com.project.speakmate.ui.placement

import com.project.speakmate.domain.model.PlacementStep
import com.project.speakmate.ui.placement.state.PlacementNextTask

object PlacementNextTaskMapper {
    fun classify(current: PlacementStep, next: PlacementStep): PlacementNextTask = when {
        next.level > current.level -> PlacementNextTask.HIGHER_LEVEL
        next.level < current.level -> PlacementNextTask.LOWER_LEVEL
        else -> PlacementNextTask.SAME_LEVEL
    }
}
