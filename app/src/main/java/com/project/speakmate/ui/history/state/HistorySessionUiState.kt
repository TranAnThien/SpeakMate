package com.project.speakmate.ui.history.state

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class HistorySessionUiState(
    val id: String = "",
    val metadata: String = "",
    val title: String = "",
    val resultLabel: String = "",
    val result: HistoryResultPresentation = HistoryResultPresentation.COMPLETED
)
