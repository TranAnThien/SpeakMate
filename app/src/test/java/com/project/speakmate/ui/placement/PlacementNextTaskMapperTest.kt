package com.project.speakmate.ui.placement

import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.PlacementStep
import com.project.speakmate.domain.rules.PlacementRules
import com.project.speakmate.ui.placement.state.PlacementNextTask
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class PlacementNextTaskMapperTest(
    private val current: PlacementStep,
    private val outcome: AssessmentResult,
    private val next: PlacementStep,
    private val expected: PlacementNextTask
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0} / {1} -> {2}")
        fun transitions(): List<Array<Any>> {
            val passing = listOf(
                arrayOf<Any>(PlacementStep.INITIAL_A2, AssessmentResult.PASS, PlacementStep.B1_PROBE, PlacementNextTask.HIGHER_LEVEL),
                arrayOf<Any>(PlacementStep.B1_PROBE, AssessmentResult.PASS, PlacementStep.B2_PROBE, PlacementNextTask.HIGHER_LEVEL),
                arrayOf<Any>(PlacementStep.B2_PROBE, AssessmentResult.PASS, PlacementStep.B2_CONFIRMATION, PlacementNextTask.SAME_LEVEL)
            )
            val remaining = listOf(
                Triple(PlacementStep.INITIAL_A2, PlacementStep.A2_CONFIRMATION, PlacementNextTask.SAME_LEVEL),
                Triple(PlacementStep.B1_PROBE, PlacementStep.A2_CONFIRMATION, PlacementNextTask.LOWER_LEVEL),
                Triple(PlacementStep.B2_PROBE, PlacementStep.B1_CONFIRMATION, PlacementNextTask.LOWER_LEVEL),
                Triple(PlacementStep.B2_CONFIRMATION, PlacementStep.B1_CONFIRMATION, PlacementNextTask.LOWER_LEVEL),
                Triple(PlacementStep.B1_CONFIRMATION, PlacementStep.A2_CONFIRMATION, PlacementNextTask.LOWER_LEVEL),
                Triple(PlacementStep.A2_CONFIRMATION, PlacementStep.A1_CONFIRMATION, PlacementNextTask.LOWER_LEVEL)
            ).flatMap { (step, nextStep, classification) ->
                listOf(AssessmentResult.PARTIAL, AssessmentResult.FAIL).map { arrayOf<Any>(step, it, nextStep, classification) }
            }
            return passing + remaining
        }
    }

    @Test
    fun classifiesEveryNonterminalOutcomeFromTheApprovedRoutingTable() {
        assertEquals(next, PlacementRules.getNextStep(current, outcome))
        assertEquals(expected, PlacementNextTaskMapper.classify(current, next))
    }
}
