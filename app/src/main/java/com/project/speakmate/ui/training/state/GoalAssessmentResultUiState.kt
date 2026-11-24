package com.project.speakmate.ui.training.state

import com.project.speakmate.domain.model.InsufficientEvidenceReason
import com.project.speakmate.domain.model.PracticeSaveFailure
import com.project.speakmate.domain.rules.PracticeSaveRetryRules

data class GoalAssessmentResultUiState(
    val presentation: GoalAssessmentPresentation = GoalAssessmentPresentation.PROCESSING,
    val feedback: String = "",
    val nextGoalId: String? = null,
    val isSaved: Boolean = false,
    val isReplay: Boolean = false,
    val isSaveFailed: Boolean = false,
    val isSaving: Boolean = false,
    val isUnavailable: Boolean = false,
    val hasLoadError: Boolean = false,
    val isAwaitingGrading: Boolean = false,
    val saveFailure: PracticeSaveFailure? = null,
    val insufficientReason: InsufficientEvidenceReason? = null,
    val resultActions: List<UnitResultActionUiState> = emptyList(),
    val criteria: List<UnitResultCriterionUiState> = emptyList()
) {
    val canContinue: Boolean
        get() = !hasLoadError && !isSaving && (isSaved || (isSaveFailed && canRetrySave))

    val canRetrySave: Boolean
        get() = saveFailure?.let(PracticeSaveRetryRules::canRetry) != false
}
