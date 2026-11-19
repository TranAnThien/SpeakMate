package com.project.speakmate.ui.history.state

import androidx.compose.runtime.Immutable
import com.project.speakmate.ui.components.AssessmentCriterionType

@Immutable
data class SessionCriterionUiState(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val isPassed: Boolean = false,
    val criterion: AssessmentCriterionType? = null
)
