package com.project.speakmate.ui.placement.state

import com.project.speakmate.domain.model.InsufficientEvidenceReason
import com.project.speakmate.domain.model.ReassessmentState
import com.project.speakmate.domain.model.ReassessmentTransition
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.PracticeRecoveryReason

data class ReassessmentUiState(
    val phase: ReassessmentPhase = ReassessmentPhase.LOADING,
    val currentLevel: SpeakingLevel = SpeakingLevel.NOT_ASSESSED,
    val nextLevel: SpeakingLevel = SpeakingLevel.NOT_ASSESSED,
    val assessment: ReassessmentState? = null,
    val scenario: Scenario? = null,
    val result: ReassessmentTransition? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val isSaveFailed: Boolean = false,
    val hasReachedAudioLimit: Boolean = false,
    val pendingGradingReason: InsufficientEvidenceReason? = null,
    val recoveryProblem: PracticeRecoveryReason? = null
)
