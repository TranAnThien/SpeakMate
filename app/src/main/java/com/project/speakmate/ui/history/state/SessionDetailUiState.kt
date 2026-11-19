package com.project.speakmate.ui.history.state

import androidx.compose.runtime.Immutable

@Immutable
data class SessionDetailUiState(
    val presentation: SessionDetailPresentation = SessionDetailPresentation.LOADING,
    val title: String = "",
    val metadata: String = "",
    val criteriaHeading: String = "",
    val requiredActions: List<SessionActionUiState> = emptyList(),
    val criteria: List<SessionCriterionUiState> = emptyList(),
    val messages: List<SessionMessageUiState> = emptyList(),
    val result: HistoryResultPresentation? = null,
    val feedbackSummary: String? = null
)
