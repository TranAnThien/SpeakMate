package com.project.speakmate.domain.rules

import com.project.speakmate.domain.model.RequiredAction
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.model.ScenarioVariant
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingActivity
import com.project.speakmate.domain.model.TrainingActivityScenario

object VariantTestFixtures {
    private const val DIFFICULTY_CODE = "standard"
    private const val PASS_CONDITION_CODE = "speaking-and-actions"
    private const val COMMUNICATIVE_GOAL = "Order a drink"
    private const val SECOND_REQUIRED_ACTION_ID = "RA2_SPECIFY_SIZE"
    private const val VARIANT_ID = "retry-wording"

    fun getOriginal(
        activity: TrainingActivity = TrainingActivity.UnitActivity(0, 0),
        scenarioId: String = "order-drink"
    ): TrainingActivityScenario = TrainingActivityScenario(
        scenario = Scenario(scenarioId, "Cafe", "Order at a cafe", "What would you like to drink?"),
        activity = activity,
        targetLevel = SpeakingLevel.A2,
        communicativeGoal = COMMUNICATIVE_GOAL,
        requiredActions = listOf(
            RequiredAction(AssessmentTestFixtures.REQUIRED_ACTION_ID, "Orders a drink"),
            RequiredAction(SECOND_REQUIRED_ACTION_ID, "Specifies a size")
        ),
        difficultyCode = DIFFICULTY_CODE,
        passConditionCode = PASS_CONDITION_CODE
    )

    fun getVariant(original: TrainingActivityScenario, variantId: String = VARIANT_ID): ScenarioVariant =
        ScenarioVariant(
            variantId = variantId,
            scenarioId = original.scenario.scenarioId,
            activity = original.activity,
            promptTemplate = "Which drink can I get you today?",
            targetLevel = original.targetLevel,
            communicativeGoal = original.communicativeGoal,
            requiredActionIds = original.requiredActions.map { it.requiredActionId }.reversed().toSet(),
            difficultyCode = original.difficultyCode,
            passConditionCode = original.passConditionCode
        )
}
