package com.project.speakmate.ui.placement.state

import androidx.compose.runtime.Immutable

@Immutable
data class PlacementSkillUiState(
    val feedback: String = "",
    val hasPassed: Boolean = false
)
