package com.project.speakmate.ui.history.state

import androidx.compose.runtime.Immutable
import com.project.speakmate.domain.model.PracticeCompletionStatus
import com.project.speakmate.domain.model.PracticeType

@Immutable
data class HistoryPendingSessionUiState(
    val id: String = "",
    val title: String = "",
    val practiceType: PracticeType = PracticeType.FREE_PRACTICE,
    val performedAt: String = "",
    val performedDate: String = "",
    val status: PracticeCompletionStatus = PracticeCompletionStatus.GRADING
)
