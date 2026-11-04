package com.project.speakmate.ui.training.state

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class PathCompletionUiState(
    val levelLabel: String = "",
    val nextLevelLabel: String = "",
    val isHighestLevel: Boolean = false,
    val description: String = "",
    val noticeTitle: String = "",
    val notice: String = ""
)
