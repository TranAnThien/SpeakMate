package com.project.speakmate.ui.practice.state

import com.project.speakmate.domain.model.PracticeSaveFailure
import com.project.speakmate.domain.rules.PracticeSaveRetryRules
import com.project.speakmate.ui.components.TranscriptMessageUiState

data class PracticeSummaryUiState(
    val scenarioTitle: String = "",
    val sessionDescription: String = "",
    val presentation: PracticeSummaryPresentation = PracticeSummaryPresentation.FINALIZING,
    val saveFailure: PracticeSaveFailure? = null,
    val messages: List<TranscriptMessageUiState> = emptyList(),
    val isTranscriptVisible: Boolean = false,
    val canReload: Boolean = true
) {
    val canRetrySave: Boolean
        get() = saveFailure?.let(PracticeSaveRetryRules::canRetryManually) != false
}
