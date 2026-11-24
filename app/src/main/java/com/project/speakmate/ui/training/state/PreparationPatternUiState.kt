package com.project.speakmate.ui.training.state

import androidx.compose.runtime.Immutable

@Immutable
data class PreparationPatternUiState(
    val id: String = "",
    val pattern: String = "",
    val meaning: String = "",
    val usage: String = "",
    val examples: List<PreparationExampleUiState> = emptyList()
)
