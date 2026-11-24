package com.project.speakmate.ui.training.state

import com.project.speakmate.domain.model.SpeakingLevel

data class PathCompletionUiState(
    val isLoading: Boolean = true,
    val hasLoadError: Boolean = false,
    val isCompleted: Boolean = false,
    val level: SpeakingLevel = SpeakingLevel.NOT_ASSESSED,
    val nextLevel: SpeakingLevel? = null
) {
    val isHighestLevel: Boolean
        get() = level == SpeakingLevel.B2
}
