package com.project.speakmate.ui.placement.state

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class PlacementResultUiState(
    val level: String = "",
    val levelLabel: String = "",
    val range: PlacementSkillUiState = PlacementSkillUiState(),
    val accuracy: PlacementSkillUiState = PlacementSkillUiState(),
    val fluency: PlacementSkillUiState = PlacementSkillUiState(),
    val interaction: PlacementSkillUiState = PlacementSkillUiState(),
    val coherence: PlacementSkillUiState = PlacementSkillUiState()
)
