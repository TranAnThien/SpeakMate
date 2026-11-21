package com.project.speakmate.ui.history.state

import androidx.compose.runtime.Immutable

@Immutable
data class HistoryGroupUiState(
    val id: String = "",
    val title: String = "",
    val sessions: List<HistorySessionUiState> = emptyList()
)
