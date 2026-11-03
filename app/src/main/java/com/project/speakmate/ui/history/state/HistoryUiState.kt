package com.project.speakmate.ui.history.state

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import com.project.speakmate.ui.components.ContentFilterUiState

@Keep
@Immutable
data class HistoryUiState(
    val presentation: HistoryPresentation = HistoryPresentation.POPULATED,
    val filters: List<ContentFilterUiState> = emptyList(),
    val selectedFilterId: String = "",
    val groups: List<HistoryGroupUiState> = emptyList()
)
