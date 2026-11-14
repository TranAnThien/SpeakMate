package com.project.speakmate.domain.model

import com.project.speakmate.domain.rules.TrainingProgressRules

data class TrainingPath(
    val pathId: String,
    val targetLevel: SpeakingLevel,
    val title: String,
    val goals: List<TrainingGoal>
) {
    fun getGoalStatus(goalIndex: Int, progress: TrainingProgress): ContentStatus {
        return TrainingProgressRules.getGoalStatus(this, progress, goalIndex)
    }
}
