package com.project.speakmate.ui.history.state

import androidx.compose.runtime.Immutable
import com.project.speakmate.domain.model.HistoryReadFailure
import com.project.speakmate.domain.model.PracticeCompletionStatus
import com.project.speakmate.domain.model.PracticeSaveFailure
import com.project.speakmate.domain.rules.PracticeSaveRetryRules
import com.project.speakmate.ui.components.assessment.AssessmentCriterionUiState
import com.project.speakmate.ui.components.transcript.TranscriptMessageUiState

@Immutable
data class SessionDetailUiState(
    val presentation: SessionDetailPresentation = SessionDetailPresentation.LOADING,
    val title: String = "",
    val startedAtMillis: Long? = null,
    val duration: String = "",
    val requiredActions: List<SessionActionUiState> = emptyList(),
    val criteria: List<AssessmentCriterionUiState> = emptyList(),
    val messages: List<TranscriptMessageUiState> = emptyList(),
    val result: HistoryResultPresentation? = null,
    val isAssessed: Boolean = false,
    val feedbackSummary: String? = null,
    val isTranscriptLoading: Boolean = false,
    val isTemporary: Boolean = false,
    val isSaved: Boolean = false,
    val saveFailure: PracticeSaveFailure? = null,
    val isTranscriptTemporary: Boolean = false,
    val hasSavedRefreshError: Boolean = false,
    val hasPendingContent: Boolean = true,
    val pendingStatus: PracticeCompletionStatus? = null,
    val loadFailure: HistoryReadFailure? = null,
    val transcriptFailure: HistoryReadFailure? = null
) {
    val canRetrySave: Boolean
        get() = saveFailure?.let(PracticeSaveRetryRules::canRetry) != false
}
