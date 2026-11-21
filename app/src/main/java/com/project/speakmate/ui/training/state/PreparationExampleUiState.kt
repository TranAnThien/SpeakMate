package com.project.speakmate.ui.training.state

import androidx.compose.runtime.Immutable

@Immutable
data class PreparationExampleUiState(
    val sentence: String = "",
    val translation: String = ""
)
