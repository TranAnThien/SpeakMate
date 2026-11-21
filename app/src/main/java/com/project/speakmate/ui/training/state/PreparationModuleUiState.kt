package com.project.speakmate.ui.training.state

import androidx.compose.runtime.Immutable

@Immutable
data class PreparationModuleUiState(
    val section: PreparationSection = PreparationSection.VOCABULARY,
    val title: String = "",
    val description: String = ""
)
