package com.project.speakmate.domain.rules

import com.project.speakmate.domain.model.GoalAssessment
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingGoal
import com.project.speakmate.domain.model.TrainingPath
import com.project.speakmate.domain.model.TrainingUnit
import com.project.speakmate.domain.model.UnitPreparation

object TrainingProgressTestFixtures {
    private const val FIRST_GOAL_UNIT_COUNT = 3
    private const val SECOND_GOAL_UNIT_COUNT = 2

    fun getPath(
        unitCounts: List<Int> = listOf(FIRST_GOAL_UNIT_COUNT, SECOND_GOAL_UNIT_COUNT, 1)
    ): TrainingPath = TrainingPath(
        pathId = "training_path",
        targetLevel = SpeakingLevel.A2,
        title = "Training path",
        goals = unitCounts.mapIndexed { goalIndex, unitCount ->
            TrainingGoal(
                goalId = "goal_$goalIndex",
                title = "Goal $goalIndex",
                canDoDescription = "",
                goalAssessment = GoalAssessment("assessment_$goalIndex", "Goal assessment", emptyList()),
                units = List(unitCount) { unitIndex ->
                    TrainingUnit(
                        unitId = "unit_${goalIndex}_$unitIndex",
                        scenarioId = "scenario_${goalIndex}_$unitIndex",
                        title = "Unit $unitIndex",
                        communicativeFocus = "",
                        preparation = UnitPreparation(emptyList(), emptyList(), emptyList()),
                        requiredActions = emptyList()
                    )
                }
            )
        }
    )
}
