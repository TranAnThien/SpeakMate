package com.project.speakmate.domain.rules

import com.project.speakmate.domain.model.AssessmentOutcome
import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.RequiredAction
import com.project.speakmate.domain.model.RequiredActionJudgement
import com.project.speakmate.domain.model.ScenarioSelection
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingActivity
import com.project.speakmate.domain.model.TrainingProgress
import com.project.speakmate.domain.model.TrainingProgressDecision
import com.project.speakmate.domain.model.TrainingStage
import com.project.speakmate.domain.model.VariantProblem
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VariantRulesTest {
    companion object {
        private const val RANDOM_SEED = 42
        private const val OTHER_ACTION_ID = "different-action"
    }

    private val original = VariantTestFixtures.getOriginal()
    private val variant = VariantTestFixtures.getVariant(original)
    private val rules = VariantRules

    @Test
    fun acceptsNewWordingWhenInvariantsMatchRegardlessOfActionOrder() {
        assertNull(rules.validate(original, variant))
    }

    @Test
    fun identifiesEachChangedInvariant() {
        val cases = listOf(
            variant.copy(activity = TrainingActivity.GoalAssessmentActivity(0)) to VariantProblem.DIFFERENT_ACTIVITY,
            variant.copy(scenarioId = "another-scenario") to VariantProblem.DIFFERENT_SCENARIO,
            variant.copy(targetLevel = SpeakingLevel.B1) to VariantProblem.DIFFERENT_TARGET_LEVEL,
            variant.copy(communicativeGoal = "Make a booking") to VariantProblem.DIFFERENT_COMMUNICATIVE_GOAL,
            variant.copy(difficultyCode = "harder") to VariantProblem.DIFFERENT_DIFFICULTY,
            variant.copy(passConditionCode = "speaking-only") to VariantProblem.DIFFERENT_PASS_CONDITION
        )
        for ((candidate, problem) in cases) {
            assertEquals(problem, rules.validate(original, candidate))
        }
    }

    @Test
    fun rejectsMissingExtraAndReplacedRequiredActionIds() {
        val actionIds = variant.requiredActionIds
        val subsets = listOf(
            actionIds - AssessmentTestFixtures.REQUIRED_ACTION_ID,
            actionIds + OTHER_ACTION_ID,
            actionIds - AssessmentTestFixtures.REQUIRED_ACTION_ID + OTHER_ACTION_ID
        )
        for (ids in subsets) {
            assertEquals(VariantProblem.DIFFERENT_REQUIRED_ACTIONS, rules.validate(original, variant.copy(requiredActionIds = ids)))
        }
    }

    @Test
    fun excludesInvalidVariantsAndAvoidsThePreviouslyUsedVariant() {
        val invalid = variant.copy(variantId = "invalid", requiredActionIds = emptySet())
        val alternate = VariantTestFixtures.getVariant(original, "alternate")
        assertEquals(
            ScenarioSelection.Variant(alternate),
            rules.selectForRetry(original, listOf(variant, invalid, alternate), variant.variantId, Random(RANDOM_SEED))
        )
    }

    @Test
    fun fallsBackToTheOriginalWhenThereIsNoEligibleAlternative() {
        val invalid = variant.copy(variantId = "invalid", requiredActionIds = emptySet())
        val fallback = ScenarioSelection.Original(original.scenario)
        assertEquals(fallback, rules.selectForRetry(original, emptyList(), null, Random(RANDOM_SEED)))
        assertEquals(fallback, rules.selectForRetry(original, listOf(invalid), null, Random(RANDOM_SEED)))
        assertEquals(fallback, rules.selectForRetry(original, listOf(variant, invalid), variant.variantId, Random(RANDOM_SEED)))
    }

    @Test
    fun selectsTheSameValidVariantWithTheSameSeed() {
        val candidates = listOf(variant, VariantTestFixtures.getVariant(original, "alternate"))
        val selection = rules.selectForRetry(original, candidates, null, Random(RANDOM_SEED))
        assertTrue(selection is ScenarioSelection.Variant)
        assertEquals(selection, rules.selectForRetry(original, candidates, null, Random(RANDOM_SEED)))
    }

    @Test
    fun gradesRetryWithTheOriginalRequiredActionsIncludingTheirDescriptions() {
        val selected = rules.selectForRetry(original, listOf(variant), null, Random(RANDOM_SEED))
        assertEquals(ScenarioSelection.Variant(variant), selected)
        val actions = rules.getRequiredActions(original)
        assertEquals(original.requiredActions, actions)
        assertEquals(
            AssessmentResult.FAIL,
            assess(actions, isFirstActionComplete = false).result
        )
    }

    @Test
    fun passingASelectedVariantAdvancesTheSameUnitOrGoalAssessmentAsTheOriginal() {
        val path = TrainingProgressTestFixtures.getPath()
        val goal = path.goals.first()
        val activities = listOf(
            Triple(TrainingActivity.UnitActivity(0, 0), goal.units.first().scenarioId, TrainingStage.UNIT),
            Triple(TrainingActivity.GoalAssessmentActivity(0), goal.goalAssessment.scenarioId, TrainingStage.GOAL_ASSESSMENT)
        )
        for ((activity, scenarioId, stage) in activities) {
            val activityOriginal = VariantTestFixtures.getOriginal(activity, scenarioId)
            val alternate = VariantTestFixtures.getVariant(activityOriginal)
            val selection = rules.selectForRetry(activityOriginal, listOf(alternate), null, Random(RANDOM_SEED))
            assertTrue(selection is ScenarioSelection.Variant)
            if (selection !is ScenarioSelection.Variant) return
            val progress = TrainingProgress(0, if (stage == TrainingStage.UNIT) 0 else goal.units.lastIndex, stage)
            val originalOutcome = assess(activityOriginal.requiredActions)
            val retryOutcome = assess(rules.getRequiredActions(activityOriginal))
            assertEquals(AssessmentResult.PASS, retryOutcome.result)
            val expected = TrainingProgressRules.applyOutcome(path, progress, activity, originalOutcome)
            assertTrue(expected is TrainingProgressDecision.Advanced)
            assertEquals(expected, TrainingProgressRules.applyOutcome(path, progress, selection.variant.activity, retryOutcome))
        }
    }

    private fun assess(
        actions: List<RequiredAction>,
        isFirstActionComplete: Boolean = true
    ): AssessmentOutcome.Evaluated {
        val (evidence, evaluation) = AssessmentTestFixtures.getPassEvidenceSet()
        val judgements = actions.associate { action ->
            val isCompleted = isFirstActionComplete || action.requiredActionId != AssessmentTestFixtures.REQUIRED_ACTION_ID
            action.requiredActionId to RequiredActionJudgement(isCompleted, AssessmentTestFixtures.USER_QUOTE)
        }
        return AssessmentRules.assess(evidence.copy(requiredActions = actions), evaluation.copy(requiredActions = judgements))
            as AssessmentOutcome.Evaluated
    }
}
