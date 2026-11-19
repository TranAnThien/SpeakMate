package com.project.speakmate.ui.training.state

import androidx.compose.runtime.Immutable
import com.project.speakmate.domain.model.TrainingActivityScenario

@Immutable
data class GoalAssessmentUiState(
    val presentation: GoalAssessmentPresentation = GoalAssessmentPresentation.INTRODUCTION,
    val title: String = "",
    val description: String = "",
    val requiredActions: List<GoalAssessmentActionUiState> = emptyList(),
    val feedbackTitle: String = "",
    val feedback: String = "",
    val status: TrainingContentStatus = TrainingContentStatus.LOADING,
    val activity: TrainingActivityScenario? = null,
    val isReplay: Boolean = false,
    val isLocked: Boolean = false,
    val isPrimaryEnabled: Boolean = true,
    val isSaveFailed: Boolean = false,
    val isSaving: Boolean = false
)
