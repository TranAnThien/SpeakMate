package com.project.speakmate.ui.training.state

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class TrainingUnitUiState(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val status: TrainingProgressStatus = TrainingProgressStatus.LOCKED,
    val isAssessment: Boolean = false,
    val canOpen: Boolean = false
)
