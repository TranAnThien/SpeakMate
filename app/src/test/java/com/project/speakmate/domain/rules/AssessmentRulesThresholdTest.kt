package com.project.speakmate.domain.rules

import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.SpeakingCriteriaResult
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AssessmentRulesThresholdTest(
    private val criteria: SpeakingCriteriaResult,
    private val expected: AssessmentResult
) {
    companion object {
        private const val CRITERIA_COUNT = 5
        private const val PASS_COUNT = 4
        private const val PARTIAL_COUNT = 3

        @JvmStatic
        @Parameterized.Parameters
        fun getCombinations(): List<Array<Any>> = (0 until (1 shl CRITERIA_COUNT)).map { combination ->
            val values = List(CRITERIA_COUNT) { combination and (1 shl it) != 0 }
            val expected = when (values.count { it }) {
                in PASS_COUNT..CRITERIA_COUNT -> AssessmentResult.PASS
                PARTIAL_COUNT -> AssessmentResult.PARTIAL
                else -> AssessmentResult.FAIL
            }
            arrayOf(SpeakingCriteriaResult(values[0], values[1], values[2], values[3], values[4]), expected)
        }
    }

    @Test
    fun everyBooleanCombinationMatchesThreshold() {
        assertEquals(expected, AssessmentRules.calculateResult(criteria, emptyList(), emptyMap()))
    }
}
