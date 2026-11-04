package com.project.speakmate.ui.training.state

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class GoalDetailUiState(
    val title: String = "",
    val description: String = "",
    val units: List<TrainingUnitUiState> = emptyList(),
    val canPrepareCurrentUnit: Boolean = false
) {
    val hasUnits: Boolean get() = units.isNotEmpty()
}
