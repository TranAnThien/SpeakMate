package com.project.speakmate.ui.placement.state

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class PlacementSkillUiState(
    val feedback: String = "",
    val hasPassed: Boolean = false
)
