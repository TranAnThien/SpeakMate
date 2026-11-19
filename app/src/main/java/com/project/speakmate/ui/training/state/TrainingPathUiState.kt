package com.project.speakmate.ui.training.state

import androidx.compose.runtime.Immutable
import com.project.speakmate.domain.model.ProgressProblem

@Immutable
data class TrainingPathUiState(
    val status: TrainingContentStatus = TrainingContentStatus.LOADING,
    val level: String = "",
    val goals: List<TrainingGoalUiState> = emptyList(),
    val canContinue: Boolean = false,
    val progressProblem: ProgressProblem? = null
)
