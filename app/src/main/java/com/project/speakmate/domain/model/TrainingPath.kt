package com.project.speakmate.domain.model

data class TrainingPath(
    val pathId: String,
    val targetLevel: SpeakingLevel,
    val title: String,
    val goals: List<TrainingGoal>
) {
    fun getGoalStatus(goalIndex: Int, progress: TrainingProgress): ContentStatus {
        return when {
            goalIndex < progress.currentGoalOrder -> ContentStatus.COMPLETED
            goalIndex == progress.currentGoalOrder -> ContentStatus.CURRENT
            else -> ContentStatus.LOCKED
        }
    }
}