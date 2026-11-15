package com.project.speakmate.domain.rules

import com.project.speakmate.domain.model.AssessmentTask
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingProgress
import com.project.speakmate.domain.model.TrainingStage
import com.project.speakmate.domain.model.User

object ReassessmentTestFixtures {
    const val RANDOM_SEED = 42
    const val ASSESSED_AT_MILLIS = 10_000L
    private const val FINAL_GOAL_INDEX = 2

    fun getUser(
        level: SpeakingLevel,
        progress: TrainingProgress? = TrainingProgress(FINAL_GOAL_INDEX, 0, TrainingStage.PATH_COMPLETED)
    ): User = User(
        userId = "reassessment-user",
        displayName = "",
        email = "",
        photoUrl = "",
        level = level,
        trainingProgress = progress
    )

    fun getTasks(): List<AssessmentTask> = PlacementTestFixtures.getTasks()

    fun getTask(level: SpeakingLevel, index: Int = 0): AssessmentTask =
        getTasks().filter { it.targetLevel == level }[index]
}
