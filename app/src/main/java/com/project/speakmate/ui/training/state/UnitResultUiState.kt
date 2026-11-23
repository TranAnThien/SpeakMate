package com.project.speakmate.ui.training.state

import com.project.speakmate.domain.model.PracticeSaveFailure
import com.project.speakmate.domain.rules.PracticeSaveRetryRules
import androidx.compose.runtime.Immutable

@Immutable
data class UnitResultUiState(
    val presentation: UnitResultPresentation = UnitResultPresentation.PROCESSING,
    val title: String = "",
    val explanation: String = "",
    val criteriaHeading: String = "",
    val feedback: String = "",
    val requiredActions: List<UnitResultActionUiState> = emptyList(),
    val criteria: List<UnitResultCriterionUiState> = emptyList(),
    val saveFailed: Boolean = false,
    val isSaving: Boolean = false,
    val isUnavailable: Boolean = false,
    val isSaved: Boolean = false,
    val isReplay: Boolean = false,
    val isAwaitingGrading: Boolean = false,
    val saveFailure: PracticeSaveFailure? = null,
    val insufficientReason: com.project.speakmate.domain.model.InsufficientEvidenceReason? = null
) {
    val canRetrySave: Boolean
        get() = saveFailure?.let(PracticeSaveRetryRules::canRetryManually) != false
}
