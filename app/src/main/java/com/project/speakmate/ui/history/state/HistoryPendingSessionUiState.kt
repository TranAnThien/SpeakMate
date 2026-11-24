package com.project.speakmate.ui.history.state

import androidx.compose.runtime.Immutable
import com.project.speakmate.domain.model.PracticeCompletionStatus
import com.project.speakmate.domain.model.PracticeSaveFailure
import com.project.speakmate.domain.model.PracticeType
import com.project.speakmate.domain.rules.PracticeSaveRetryRules

@Immutable
data class HistoryPendingSessionUiState(
    val id: String = "",
    val title: String = "",
    val practiceType: PracticeType? = PracticeType.FREE_PRACTICE,
    val startedAtMillis: Long? = null,
    val status: PracticeCompletionStatus = PracticeCompletionStatus.GRADING,
    val saveFailure: PracticeSaveFailure? = null
) {
    val canRetrySave: Boolean
        get() = saveFailure?.let(PracticeSaveRetryRules::canRetry) != false
}
