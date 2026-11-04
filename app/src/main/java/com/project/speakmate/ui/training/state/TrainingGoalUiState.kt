package com.project.speakmate.ui.training.state

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class TrainingGoalUiState(
    val id: String = "",
    val label: String = "",
    val title: String = "",
    val summary: String = "",
    val unitsHeading: String = "",
    val status: TrainingProgressStatus = TrainingProgressStatus.LOCKED,
    val isExpanded: Boolean = false,
    val units: List<TrainingUnitUiState> = emptyList()
)
