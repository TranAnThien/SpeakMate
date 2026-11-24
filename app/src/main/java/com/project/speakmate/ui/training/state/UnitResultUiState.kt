package com.project.speakmate.ui.training.state

import androidx.compose.runtime.Immutable
import com.project.speakmate.domain.model.InsufficientEvidenceReason
import com.project.speakmate.domain.model.PracticeSaveFailure
import com.project.speakmate.domain.rules.PracticeSaveRetryRules

@Immutable
data class UnitResultUiState(
    val presentation: UnitResultPresentation = UnitResultPresentation.PROCESSING,
    val title: String = "",
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
    val insufficientReason: InsufficientEvidenceReason? = null
) {
    val canRetrySave: Boolean
        get() = saveFailure?.let(PracticeSaveRetryRules::canRetry) != false
}
