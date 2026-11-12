package com.project.speakmate.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeakingCriteriaResultTest {

    @Test
    fun calculateSpeakingResult_returnsPASS_when4Or5CriteriaMet() {
        val result5 = SpeakingCriteriaResult(
            rangeMet = true,
            accuracyMet = true,
            fluencyMet = true,
            interactionMet = true,
            coherenceMet = true
        )
        assertEquals(AssessmentResult.PASS, result5.calculateSpeakingResult())

        val result4 = SpeakingCriteriaResult(
            rangeMet = true,
            accuracyMet = true,
            fluencyMet = true,
            interactionMet = true,
            coherenceMet = false
        )
        assertEquals(AssessmentResult.PASS, result4.calculateSpeakingResult())
    }

    @Test
    fun calculateSpeakingResult_returnsPARTIAL_when3CriteriaMet() {
        val result3 = SpeakingCriteriaResult(
            rangeMet = true,
            accuracyMet = true,
            fluencyMet = true,
            interactionMet = false,
            coherenceMet = false
        )
        assertEquals(AssessmentResult.PARTIAL, result3.calculateSpeakingResult())
    }

    @Test
    fun calculateSpeakingResult_returnsFAIL_when0To2CriteriaMet() {
        val result2 = SpeakingCriteriaResult(
            rangeMet = true,
            accuracyMet = true,
            fluencyMet = false,
            interactionMet = false,
            coherenceMet = false
        )
        assertEquals(AssessmentResult.FAIL, result2.calculateSpeakingResult())

        val result0 = SpeakingCriteriaResult(
            rangeMet = false,
            accuracyMet = false,
            fluencyMet = false,
            interactionMet = false,
            coherenceMet = false
        )
        assertEquals(AssessmentResult.FAIL, result0.calculateSpeakingResult())
    }
}
