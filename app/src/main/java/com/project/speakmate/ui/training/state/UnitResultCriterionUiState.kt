package com.project.speakmate.ui.training.state

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class UnitResultCriterionUiState(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val evaluation: UnitResultEvaluation = UnitResultEvaluation.NOT_EVALUATED
)
