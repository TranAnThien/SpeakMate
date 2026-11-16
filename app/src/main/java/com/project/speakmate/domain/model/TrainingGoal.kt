package com.project.speakmate.domain.model

import com.project.speakmate.domain.rules.TrainingProgressRules

data class TrainingGoal(
    val goalId: String,
    val title: String,
    val canDoDescription: String,
    val goalAssessment: GoalAssessment,
    val units: List<TrainingUnit>
) {
    fun getUnitStatus(goalIndex: Int, unitIndex: Int, progress: TrainingProgress): ContentStatus {
        return TrainingProgressRules.getUnitStatus(this, progress, goalIndex, unitIndex)
    }
}
