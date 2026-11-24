package com.project.speakmate.ui.training.state

import androidx.compose.runtime.Immutable
import com.project.speakmate.ui.components.assessment.AssessmentCriterionType

@Immutable
data class UnitResultCriterionUiState(
    val id: String = "",
    val description: String = "",
    val evaluation: UnitResultEvaluation = UnitResultEvaluation.NOT_EVALUATED,
    val criterion: AssessmentCriterionType = AssessmentCriterionType.RANGE
)
