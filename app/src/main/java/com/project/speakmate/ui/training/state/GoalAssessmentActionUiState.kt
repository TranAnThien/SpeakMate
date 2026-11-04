package com.project.speakmate.ui.training.state

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class GoalAssessmentActionUiState(
    val id: String = "",
    val text: String = ""
)
