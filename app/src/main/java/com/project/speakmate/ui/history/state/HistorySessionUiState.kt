package com.project.speakmate.ui.history.state

import androidx.compose.runtime.Immutable
import com.project.speakmate.domain.model.PracticeType

@Immutable
data class HistorySessionUiState(
    val id: String = "",
    val metadata: String = "",
    val title: String = "",
    val resultLabel: String = "",
    val result: HistoryResultPresentation = HistoryResultPresentation.COMPLETED,
    val practiceType: PracticeType? = null,
    val duration: String = "",
    val performedAt: String = ""
)
