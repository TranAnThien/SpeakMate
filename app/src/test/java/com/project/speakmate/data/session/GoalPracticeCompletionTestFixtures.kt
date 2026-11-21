package com.project.speakmate.data.session

import com.project.speakmate.domain.model.CallActivityContext
import com.project.speakmate.domain.model.TrainingActivity
import com.project.speakmate.domain.rules.TrainingProgressTestFixtures

object GoalPracticeCompletionTestFixtures {
    val path = TrainingProgressTestFixtures.getPath(listOf(1, 1)).let { original ->
        original.copy(
            targetLevel = PracticeCompletionTestFixtures.training.targetLevel,
            goals = original.goals.map { goal ->
                goal.copy(goalAssessment = goal.goalAssessment.copy(requiredActions = PracticeCompletionTestFixtures.training.requiredActions))
            }
        )
    }

    fun practice(goalIndex: Int = 0) = PracticeCompletionTestFixtures.practice().copy(
        context = CallActivityContext.Training(PracticeCompletionTestFixtures.training.copy(
            scenario = PracticeCompletionTestFixtures.scenario.copy(scenarioId = path.goals[goalIndex].goalAssessment.scenarioId),
            activity = TrainingActivity.GoalAssessmentActivity(goalIndex)
        ))
    )
}
