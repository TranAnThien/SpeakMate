package com.project.speakmate.ui.placement.state

import androidx.compose.runtime.Immutable
import com.project.speakmate.domain.model.SpeakingLevel

@Immutable
data class PlacementResultUiState(
    val level: SpeakingLevel = SpeakingLevel.NOT_ASSESSED,
    val range: PlacementSkillUiState = PlacementSkillUiState(),
    val accuracy: PlacementSkillUiState = PlacementSkillUiState(),
    val fluency: PlacementSkillUiState = PlacementSkillUiState(),
    val interaction: PlacementSkillUiState = PlacementSkillUiState(),
    val coherence: PlacementSkillUiState = PlacementSkillUiState(),
    val isLowestLevelAssigned: Boolean = false,
    val isSavingResult: Boolean = false,
    val saveFailed: Boolean = false,
    val isResultSaved: Boolean = false
)
