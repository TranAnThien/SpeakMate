package com.project.speakmate.ui.placement.state

import androidx.compose.runtime.Immutable

@Immutable
data class PromotionResultUiState(
    val presentation: PromotionResultPresentation = PromotionResultPresentation.PROMOTED,
    val title: String = "",
    val description: String = "",
    val currentLevelLabel: String = "",
    val nextLevelLabel: String = "",
    val canRetryAssessment: Boolean = true,
    val isSaving: Boolean = false,
    val isSaveFailed: Boolean = false,
    val isResultSaved: Boolean = true
)
