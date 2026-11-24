package com.project.speakmate.ui.history.state

import androidx.compose.runtime.Immutable

@Immutable
data class HistoryGroupUiState(
    val id: String = "",
    val startedAtMillis: Long = 0L,
    val sessions: List<HistorySessionUiState> = emptyList()
)
