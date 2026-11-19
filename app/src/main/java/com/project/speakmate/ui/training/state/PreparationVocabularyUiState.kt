package com.project.speakmate.ui.training.state

import androidx.compose.runtime.Immutable

@Immutable
data class PreparationVocabularyUiState(
    val id: String = "",
    val word: String = "",
    val pronunciationLabel: String = "",
    val meaning: String = "",
    val example: PreparationExampleUiState = PreparationExampleUiState()
)
