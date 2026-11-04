package com.project.speakmate.ui.training.state

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class GoalAssessmentUiState(
    val presentation: GoalAssessmentPresentation = GoalAssessmentPresentation.INTRODUCTION,
    val title: String = "",
    val description: String = "",
    val requiredActions: List<GoalAssessmentActionUiState> = emptyList(),
    val feedbackTitle: String = "",
    val feedback: String = ""
)
