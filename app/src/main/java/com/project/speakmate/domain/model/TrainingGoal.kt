package com.project.speakmate.domain.model

data class TrainingGoal(
    val goalId: String,
    val title: String,
    val canDoDescription: String,
    val goalAssessment: GoalAssessment,
    val units: List<TrainingUnit>
) {
    fun getUnitStatus(goalIndex: Int, unitIndex: Int, progress: TrainingProgress): ContentStatus {
        return when {
            goalIndex < progress.currentGoalOrder -> ContentStatus.COMPLETED
            goalIndex == progress.currentGoalOrder && unitIndex < progress.currentUnitOrder -> ContentStatus.COMPLETED
            goalIndex == progress.currentGoalOrder && unitIndex == progress.currentUnitOrder -> ContentStatus.CURRENT
            else -> ContentStatus.LOCKED
        }
    }
}