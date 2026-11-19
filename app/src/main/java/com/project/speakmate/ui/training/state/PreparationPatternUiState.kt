package com.project.speakmate.ui.training.state

import androidx.compose.runtime.Immutable

@Immutable
data class PreparationPatternUiState(
    val id: String = "",
    val label: String = "",
    val pattern: String = "",
    val meaning: String = "",
    val usage: String = "",
    val example: PreparationExampleUiState = PreparationExampleUiState(),
    val examples: List<PreparationExampleUiState> = emptyList()
)
