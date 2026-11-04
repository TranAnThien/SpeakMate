package com.project.speakmate.ui.training.state

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class TrainingPathUiState(
    val status: TrainingContentStatus = TrainingContentStatus.LOADING,
    val level: String = "",
    val goals: List<TrainingGoalUiState> = emptyList(),
    val canContinue: Boolean = false
)
