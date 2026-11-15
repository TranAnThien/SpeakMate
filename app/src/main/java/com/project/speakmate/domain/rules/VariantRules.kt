package com.project.speakmate.domain.rules

import com.project.speakmate.domain.model.RequiredAction
import com.project.speakmate.domain.model.ScenarioSelection
import com.project.speakmate.domain.model.ScenarioVariant
import com.project.speakmate.domain.model.TrainingActivityScenario
import com.project.speakmate.domain.model.VariantProblem
import kotlin.random.Random

object VariantRules {
    fun validate(original: TrainingActivityScenario, variant: ScenarioVariant): VariantProblem? = when {
        variant.activity != original.activity -> VariantProblem.DIFFERENT_ACTIVITY
        variant.scenarioId != original.scenario.scenarioId -> VariantProblem.DIFFERENT_SCENARIO
        variant.targetLevel != original.targetLevel -> VariantProblem.DIFFERENT_TARGET_LEVEL
        variant.communicativeGoal != original.communicativeGoal -> VariantProblem.DIFFERENT_COMMUNICATIVE_GOAL
        variant.requiredActionIds != original.requiredActions.map { it.requiredActionId }.toSet() ->
            VariantProblem.DIFFERENT_REQUIRED_ACTIONS
        variant.difficultyCode != original.difficultyCode -> VariantProblem.DIFFERENT_DIFFICULTY
        variant.passConditionCode != original.passConditionCode -> VariantProblem.DIFFERENT_PASS_CONDITION
        else -> null
    }

    fun selectForRetry(
        original: TrainingActivityScenario,
        variants: List<ScenarioVariant>,
        previousVariantId: String?,
        random: Random
    ): ScenarioSelection {
        val candidates = variants.filter { it.variantId != previousVariantId && validate(original, it) == null }
        val variant = candidates.randomOrNull(random) ?: return ScenarioSelection.Original(original.scenario)
        return ScenarioSelection.Variant(variant)
    }

    fun getRequiredActions(original: TrainingActivityScenario): List<RequiredAction> = original.requiredActions
}
