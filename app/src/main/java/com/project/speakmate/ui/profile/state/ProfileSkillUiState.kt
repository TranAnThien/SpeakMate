package com.project.speakmate.ui.profile.state

import com.project.speakmate.ui.components.AssessmentCriterionType
import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class ProfileSkillUiState(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val isPassed: Boolean = false,
    val criterion: AssessmentCriterionType? = null
)
