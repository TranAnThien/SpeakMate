package com.project.speakmate.ui.profile.state

import androidx.compose.runtime.Immutable
import com.project.speakmate.ui.components.AssessmentCriterionType

@Immutable
data class ProfileSkillUiState(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val isPassed: Boolean = false,
    val criterion: AssessmentCriterionType? = null
)
