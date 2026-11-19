package com.project.speakmate.ui.practice.state

import androidx.compose.runtime.Immutable
import com.project.speakmate.ui.components.ContentFilterUiState

@Immutable
data class ScenarioCatalogUiState(
    val presentation: ScenarioCatalogPresentation = ScenarioCatalogPresentation.POPULATED,
    val categories: List<ContentFilterUiState> = emptyList(),
    val selectedCategoryId: String = "",
    val scenarios: List<ScenarioItemUiState> = emptyList()
)
