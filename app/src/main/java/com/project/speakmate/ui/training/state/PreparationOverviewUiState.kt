package com.project.speakmate.ui.training.state

import androidx.compose.runtime.Immutable

@Immutable
data class PreparationOverviewUiState(
    val imageUrl: String = "",
    val communicativeFocus: String = "",
    val modules: List<PreparationModuleUiState> = emptyList(),
    val requiredActions: List<String> = emptyList()
)
