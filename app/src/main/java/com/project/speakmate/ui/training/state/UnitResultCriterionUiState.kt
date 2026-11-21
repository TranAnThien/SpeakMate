package com.project.speakmate.ui.training.state

import androidx.compose.runtime.Immutable

@Immutable
data class UnitResultCriterionUiState(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val evaluation: UnitResultEvaluation = UnitResultEvaluation.NOT_EVALUATED,
    val criterion: UnitResultCriterionType = UnitResultCriterionType.RANGE
)
