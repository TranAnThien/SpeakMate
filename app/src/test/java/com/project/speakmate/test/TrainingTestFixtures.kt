package com.project.speakmate.test

import com.project.speakmate.domain.model.GoalAssessment
import com.project.speakmate.domain.model.RequiredAction
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingGoal
import com.project.speakmate.domain.model.TrainingPath
import com.project.speakmate.domain.model.TrainingUnit
import com.project.speakmate.domain.model.UnitPreparation

object TrainingTestFixtures {
    fun getPath(): TrainingPath = TrainingPath(
        pathId = "path-a1",
        targetLevel = SpeakingLevel.A1,
        title = "A1 Path",
        goals = listOf(
            TrainingGoal(
                goalId = "goal-1",
                title = "Goal 1",
                canDoDescription = "Can do one",
                goalAssessment = GoalAssessment("assessment-1", "Goal 1 assessment", emptyList()),
                units = listOf(getUnit("unit-1"), getUnit("unit-2"))
            ),
            TrainingGoal(
                goalId = "goal-2",
                title = "Goal 2",
                canDoDescription = "Can do two",
                goalAssessment = GoalAssessment("assessment-2", "Goal 2 assessment", emptyList()),
                units = listOf(getUnit("unit-3"))
            )
        )
    )

    fun getUnit(id: String): TrainingUnit = TrainingUnit(
        unitId = id,
        scenarioId = "scenario-$id",
        title = "Title $id",
        communicativeFocus = "Focus $id",
        preparation = UnitPreparation(emptyList(), emptyList(), emptyList()),
        requiredActions = listOf(RequiredAction("action-$id", "Action $id"))
    )
}
