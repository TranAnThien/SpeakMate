package com.project.speakmate.ui.training.state

import com.project.speakmate.domain.model.SpeakingLevel

data class GoalAssessmentResultUiState(
    val presentation: GoalAssessmentPresentation = GoalAssessmentPresentation.PROCESSING,
    val feedback: String = "",
    val level: SpeakingLevel = SpeakingLevel.NOT_ASSESSED,
    val nextGoalId: String? = null,
    val isSaved: Boolean = false,
    val isSaveFailed: Boolean = false,
    val isSaving: Boolean = false,
    val isUnavailable: Boolean = false,
    val hasLoadError: Boolean = false
)
