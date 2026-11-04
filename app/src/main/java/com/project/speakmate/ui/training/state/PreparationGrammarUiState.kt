package com.project.speakmate.ui.training.state

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class PreparationGrammarUiState(
    val id: String = "",
    val title: String = "",
    val explanation: String = "",
    val structure: String = "",
    val example: PreparationExampleUiState = PreparationExampleUiState(),
    val usageNote: String = ""
)
