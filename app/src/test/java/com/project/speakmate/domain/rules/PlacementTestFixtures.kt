package com.project.speakmate.domain.rules

import com.project.speakmate.domain.model.AssessmentOutcome
import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.AssessmentTask
import com.project.speakmate.domain.model.PlacementCheckpoint
import com.project.speakmate.domain.model.PlacementStep
import com.project.speakmate.domain.model.SpeakingCriteriaResult
import com.project.speakmate.domain.model.SpeakingLevel

object PlacementTestFixtures {
    const val USER_ID = "placement-user"
    const val CREATED_AT_MILLIS = 1_000L
    const val RANDOM_SEED = 42

    fun getTasks(): List<AssessmentTask> = listOf(
        getTask(SpeakingLevel.A2, 1), getTask(SpeakingLevel.A2, 2),
        getTask(SpeakingLevel.B1, 1), getTask(SpeakingLevel.B1, 2),
        getTask(SpeakingLevel.B2, 1), getTask(SpeakingLevel.B2, 2), getTask(SpeakingLevel.A1, 1)
    )

    fun getCheckpoint(step: PlacementStep = PlacementStep.INITIAL_A2): PlacementCheckpoint {
        val task = getTasks().first { it.targetLevel == step.level }
        return PlacementRules.start(USER_ID, getTasks().first(), CREATED_AT_MILLIS)
            .copy(step = step, currentTaskId = task.taskId)
    }

    fun getOutcome(result: AssessmentResult): AssessmentOutcome.Evaluated {
        val evaluation = when (result) {
            AssessmentResult.PASS -> AssessmentTestFixtures.getPassEvidenceSet().second
            AssessmentResult.PARTIAL -> AssessmentTestFixtures.getPartialEvidenceSet().second
            AssessmentResult.FAIL -> AssessmentTestFixtures.getFailEvidenceSet().second
        }
        return AssessmentOutcome.Evaluated(
            result = result,
            criteria = SpeakingCriteriaResult(evaluation.range.isMet, evaluation.accuracy.isMet, evaluation.fluency.isMet,
                evaluation.interaction.isMet, evaluation.coherence.isMet),
            requiredActionResults = emptyMap(),
            evaluation = evaluation
        )
    }

    private fun getTask(level: SpeakingLevel, index: Int): AssessmentTask = AssessmentTask(
        taskId = "task_${level.name}_$index",
        scenarioId = "scenario_${level.name}_$index",
        title = "Placement task",
        targetLevel = level,
        communicativeGoal = "Discuss a familiar topic"
    )
}
