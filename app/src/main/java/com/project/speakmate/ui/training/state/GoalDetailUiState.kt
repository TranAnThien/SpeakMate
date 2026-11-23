package com.project.speakmate.ui.training.state

import androidx.compose.runtime.Immutable

@Immutable
data class GoalDetailUiState(
    val title: String = "",
    val description: String = "",
    val units: List<TrainingUnitUiState> = emptyList(),
    val canPrepareCurrentUnit: Boolean = false,
    val isConfigured: Boolean = true,
    val isStale: Boolean = false,
    val isLocked: Boolean = false
) {
    val hasUnits: Boolean get() = units.isNotEmpty()
}
