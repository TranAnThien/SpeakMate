package com.project.speakmate.ui.training.state

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class PreparationPatternUiState(
    val id: String = "",
    val label: String = "",
    val pattern: String = "",
    val meaning: String = "",
    val usage: String = "",
    val example: PreparationExampleUiState = PreparationExampleUiState()
)
