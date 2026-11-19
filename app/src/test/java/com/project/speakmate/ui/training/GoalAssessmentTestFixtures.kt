package com.project.speakmate.ui.training

import androidx.lifecycle.SavedStateHandle
import com.project.speakmate.data.session.PracticeCompletionTestFixtures
import com.project.speakmate.domain.model.AssessmentOutcome
import com.project.speakmate.domain.model.CallActivityContext
import com.project.speakmate.domain.model.TrainingActivity
import com.project.speakmate.domain.model.TrainingProgress
import com.project.speakmate.domain.model.TrainingStage
import com.project.speakmate.domain.model.User
import com.project.speakmate.domain.rules.PracticeCompletionRules
import com.project.speakmate.domain.rules.TrainingProgressTestFixtures
import com.project.speakmate.utils.AppConstant

object GoalAssessmentTestFixtures {
    const val ATTEMPT_ID = "goal-attempt"
    const val VIEW_MODEL_KEY = "goal-assessment"
    val scenario = PracticeCompletionTestFixtures.scenario
    val path = TrainingProgressTestFixtures.getPath(listOf(1, 1)).let { original ->
        original.copy(
            targetLevel = PracticeCompletionTestFixtures.training.targetLevel,
            goals = original.goals.map { goal ->
                goal.copy(
                    canDoDescription = "Complete the communicative goal",
                    goalAssessment = goal.goalAssessment.copy(requiredActions = PracticeCompletionTestFixtures.training.requiredActions)
                )
            }
        )
    }

    fun user(progress: TrainingProgress = TrainingProgress(0, 0, TrainingStage.GOAL_ASSESSMENT)) = User(
        userId = PracticeCompletionTestFixtures.USER_ID,
        displayName = "Student",
        email = "student@example.com",
        photoUrl = "",
        level = path.targetLevel,
        trainingProgress = progress
    )

    fun arguments(goalIndex: Int = 0, isReplay: Boolean = false) = SavedStateHandle(mapOf(
        AppConstant.GOAL_ID_ARGUMENT to path.goals[goalIndex].goalId,
        AppConstant.ATTEMPT_ID_ARGUMENT to ATTEMPT_ID,
        AppConstant.IS_REPLAY_ARGUMENT to isReplay.toString()
    ))

    fun practice(goalIndex: Int = 0) = PracticeCompletionTestFixtures.practice().let { practice ->
        val goal = path.goals[goalIndex]
        practice.copy(context = CallActivityContext.Training(PracticeCompletionTestFixtures.training.copy(
            scenario = scenario.copy(scenarioId = goal.goalAssessment.scenarioId),
            activity = TrainingActivity.GoalAssessmentActivity(goalIndex),
            communicativeGoal = goal.canDoDescription
        )))
    }

    fun completion(goalIndex: Int = 0, outcome: AssessmentOutcome = PracticeCompletionTestFixtures.evaluated()) =
        PracticeCompletionRules.createCompletion(ATTEMPT_ID, practice(goalIndex), outcome)
}
