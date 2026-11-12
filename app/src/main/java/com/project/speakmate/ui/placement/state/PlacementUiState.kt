package com.project.speakmate.ui.placement.state

import com.project.speakmate.domain.model.AssessmentTask
import com.project.speakmate.domain.model.InsufficientEvidenceReason
import com.project.speakmate.domain.model.PlacementResult
import com.project.speakmate.domain.model.PlacementStopReason
import com.project.speakmate.domain.model.Scenario

data class PlacementUiState(
    val phase: PlacementPhase = PlacementPhase.LOADING,
    val nextTask: PlacementNextTask? = null,
    val currentTask: AssessmentTask? = null,
    val scenario: Scenario? = null,
    val result: PlacementResult? = null,
    val canRetry: Boolean = false,
    val isSavingResult: Boolean = false,
    val saveFailed: Boolean = false,
    val insufficientReason: InsufficientEvidenceReason? = null,
    val stopReason: PlacementStopReason? = null,
    val hasReachedAudioLimit: Boolean = false
)
