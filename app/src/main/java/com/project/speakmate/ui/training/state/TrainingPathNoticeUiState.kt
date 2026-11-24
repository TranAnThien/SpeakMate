package com.project.speakmate.ui.training.state

import androidx.compose.runtime.Immutable

@Immutable
data class TrainingPathNoticeUiState(val notice: TrainingPathNotice, val unit: TrainingUnitUiState? = null)
