package com.project.speakmate.ui.training.state

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class UnitResultActionUiState(
    val id: String = "",
    val text: String = "",
    val evaluation: UnitResultEvaluation = UnitResultEvaluation.NOT_EVALUATED
)
