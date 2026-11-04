package com.project.speakmate.ui.training.state

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class PreparationExampleUiState(
    val sentence: String = "",
    val translation: String = ""
)
