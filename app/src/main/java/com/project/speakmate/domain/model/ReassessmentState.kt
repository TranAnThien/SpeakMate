package com.project.speakmate.domain.model

data class ReassessmentState(
    val currentLevel: SpeakingLevel,
    val nextLevel: SpeakingLevel,
    val currentTask: AssessmentTask? = null,
    val technicalRetryCount: Int = 0,
    val currentConfirmation: PassedTask? = null,
    val nextProbe: PassedTask? = null
) {
    val step: ReassessmentStep
        get() = when {
            currentConfirmation == null -> ReassessmentStep.CURRENT_CONFIRMATION
            nextProbe == null -> ReassessmentStep.NEXT_PROBE
            else -> ReassessmentStep.NEXT_CONFIRMATION
        }

    val usedScenarioIds: Set<String>
        get() = setOfNotNull(currentConfirmation?.scenarioId, nextProbe?.scenarioId)

    data class PassedTask(val scenarioId: String, val criteria: SpeakingCriteriaResult)
}
