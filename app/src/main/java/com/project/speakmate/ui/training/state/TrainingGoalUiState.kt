package com.project.speakmate.ui.training.state

import androidx.compose.runtime.Immutable

@Immutable
data class TrainingGoalUiState(
    val id: String = "",
    val title: String = "",
    val summary: String = "",
    val status: TrainingProgressStatus = TrainingProgressStatus.LOCKED,
    val isExpanded: Boolean = false,
    val units: List<TrainingUnitUiState> = emptyList(),
    val order: Int = 0,
    val isConfigured: Boolean = true
)
