package com.project.speakmate.ui.training.state

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class PreparationModuleUiState(
    val section: PreparationSection = PreparationSection.VOCABULARY,
    val title: String = "",
    val description: String = ""
)
