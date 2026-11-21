package com.project.speakmate.ui.placement.state

import androidx.compose.runtime.Immutable
import com.project.speakmate.domain.model.ReassessmentStep
import com.project.speakmate.domain.model.SpeakingLevel

@Immutable
data class PlacementStatusUiState(
    val status: PlacementStatus = PlacementStatus.PROCESSING,
    val isTimeLimitReached: Boolean = false,
    val flow: PlacementStatusFlow = PlacementStatusFlow.PLACEMENT,
    val reassessmentStep: ReassessmentStep? = null,
    val assessmentLevel: SpeakingLevel = SpeakingLevel.NOT_ASSESSED
)
