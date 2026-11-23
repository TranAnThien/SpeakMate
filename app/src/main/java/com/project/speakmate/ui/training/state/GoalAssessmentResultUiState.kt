package com.project.speakmate.ui.training.state

import com.project.speakmate.domain.model.PracticeSaveFailure
import com.project.speakmate.domain.rules.PracticeSaveRetryRules
import com.project.speakmate.domain.model.SpeakingLevel

data class GoalAssessmentResultUiState(
    val presentation: GoalAssessmentPresentation = GoalAssessmentPresentation.PROCESSING,
    val feedback: String = "",
    val level: SpeakingLevel = SpeakingLevel.NOT_ASSESSED,
    val nextGoalId: String? = null,
    val isSaved: Boolean = false,
    val isReplay: Boolean = false,
    val isSaveFailed: Boolean = false,
    val isSaving: Boolean = false,
    val isUnavailable: Boolean = false,
    val hasLoadError: Boolean = false,
    val isAwaitingGrading: Boolean = false,
    val saveFailure: PracticeSaveFailure? = null,
    val insufficientReason: com.project.speakmate.domain.model.InsufficientEvidenceReason? = null,
    val resultActions: List<UnitResultActionUiState> = emptyList(),
    val criteria: List<UnitResultCriterionUiState> = emptyList()
) {
    val canRetrySave: Boolean
        get() = saveFailure?.let(PracticeSaveRetryRules::canRetryManually) != false
}
