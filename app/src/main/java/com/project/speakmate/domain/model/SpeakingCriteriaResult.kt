package com.project.speakmate.domain.model

data class SpeakingCriteriaResult(
    val rangeMet: Boolean,
    val accuracyMet: Boolean,
    val fluencyMet: Boolean,
    val interactionMet: Boolean,
    val coherenceMet: Boolean
) {
    fun calculateSpeakingResult(): AssessmentResult {
        val metCount = listOf(rangeMet, accuracyMet, fluencyMet, interactionMet, coherenceMet).count { it }
        return when {
            metCount >= 4 -> AssessmentResult.PASS
            metCount == 3 -> AssessmentResult.PARTIAL
            else -> AssessmentResult.FAIL
        }
    }
}