package com.project.speakmate.ui.training.state

import androidx.compose.runtime.Immutable

@Immutable
data class PreparationGrammarUiState(
    val id: String = "",
    val title: String = "",
    val explanation: String = "",
    val structure: String = "",
    val example: PreparationExampleUiState = PreparationExampleUiState(),
    val usageNote: String = "",
    val examples: List<PreparationExampleUiState> = emptyList()
)
