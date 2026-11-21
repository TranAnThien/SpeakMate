package com.project.speakmate.ui.training.state

import androidx.compose.runtime.Immutable

@Immutable
data class UnitResultActionUiState(
    val id: String = "",
    val text: String = "",
    val evaluation: UnitResultEvaluation = UnitResultEvaluation.NOT_EVALUATED
)
