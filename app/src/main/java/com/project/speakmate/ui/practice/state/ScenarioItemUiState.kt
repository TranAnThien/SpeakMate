package com.project.speakmate.ui.practice.state

import androidx.compose.runtime.Immutable
import com.project.speakmate.domain.model.SpeakingLevel

@Immutable
data class ScenarioItemUiState(
    val id: String = "",
    val title: String = "",
    val categoryLabel: String = "",
    val description: String = "",
    val imageUrl: String? = null,
    val suggestedLevel: SpeakingLevel? = null,
    val estimatedDurationMinutes: Int? = null
)
