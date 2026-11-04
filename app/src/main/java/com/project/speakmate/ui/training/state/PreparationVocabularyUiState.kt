package com.project.speakmate.ui.training.state

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class PreparationVocabularyUiState(
    val id: String = "",
    val word: String = "",
    val pronunciationLabel: String = "",
    val meaning: String = "",
    val example: PreparationExampleUiState = PreparationExampleUiState()
)
