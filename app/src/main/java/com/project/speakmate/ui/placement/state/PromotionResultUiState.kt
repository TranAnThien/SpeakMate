package com.project.speakmate.ui.placement.state

import androidx.compose.runtime.Immutable
import com.project.speakmate.domain.model.ReassessmentTransition
import com.project.speakmate.domain.model.SpeakingLevel

@Immutable
data class PromotionResultUiState(
    val presentation: PromotionResultPresentation = PromotionResultPresentation.PROMOTED,
    val transition: ReassessmentTransition? = null,
    val currentLevel: SpeakingLevel = SpeakingLevel.A1,
    val nextLevel: SpeakingLevel = SpeakingLevel.A2,
    val canRetryAssessment: Boolean = true,
    val isSaving: Boolean = false,
    val isSaveFailed: Boolean = false,
    val isResultSaved: Boolean = true
)
