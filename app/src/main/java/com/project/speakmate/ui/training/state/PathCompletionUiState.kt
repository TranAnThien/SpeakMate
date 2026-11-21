package com.project.speakmate.ui.training.state

import androidx.compose.runtime.Immutable

@Immutable
data class PathCompletionUiState(
    val levelLabel: String = "",
    val nextLevelLabel: String = "",
    val isHighestLevel: Boolean = false,
    val description: String = "",
    val noticeTitle: String = "",
    val notice: String = ""
)
