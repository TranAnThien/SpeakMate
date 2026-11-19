package com.project.speakmate.ui.training.state

import androidx.compose.runtime.Immutable

@Immutable
data class TrainingUnitUiState(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val status: TrainingProgressStatus = TrainingProgressStatus.LOCKED,
    val isAssessment: Boolean = false,
    val canOpen: Boolean = false,
    val destination: TrainingActivityDestination? = null
)
