package com.project.speakmate.ui.training.state

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class PreparationOverviewUiState(
    val scenarioTitle: String = "",
    val scenarioRoles: String = "",
    val communicativeFocus: String = "",
    val modules: List<PreparationModuleUiState> = emptyList(),
    val requiredActions: List<String> = emptyList()
)
