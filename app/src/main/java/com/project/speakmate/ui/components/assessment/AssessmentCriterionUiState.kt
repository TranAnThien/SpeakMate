package com.project.speakmate.ui.components.assessment

import androidx.compose.runtime.Immutable

@Immutable
data class AssessmentCriterionUiState(
    val id: String = "",
    val isPassed: Boolean = false,
    val criterion: AssessmentCriterionType = AssessmentCriterionType.RANGE
)
