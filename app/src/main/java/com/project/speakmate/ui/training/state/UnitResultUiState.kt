package com.project.speakmate.ui.training.state

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class UnitResultUiState(
    val presentation: UnitResultPresentation = UnitResultPresentation.PROCESSING,
    val title: String = "",
    val explanation: String = "",
    val criteriaHeading: String = "",
    val feedback: String = "",
    val requiredActions: List<UnitResultActionUiState> = emptyList(),
    val criteria: List<UnitResultCriterionUiState> = emptyList()
)
