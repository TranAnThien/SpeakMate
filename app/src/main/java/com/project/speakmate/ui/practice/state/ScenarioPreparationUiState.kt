package com.project.speakmate.ui.practice.state

import androidx.compose.runtime.Immutable
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.model.SpeakingLevel

@Immutable
data class ScenarioPreparationUiState(
    val presentation: ScenarioPreparationPresentation = ScenarioPreparationPresentation.LOADING,
    val loadError: ScenarioPreparationLoadError? = null,
    val practiceId: String = "",
    val scenarioId: String = "",
    val title: String = "",
    val imageUrl: String = "",
    val context: String = "",
    val aiRole: String = "",
    val preCallHint: String = "",

    val scenario: Scenario? = null,
    val isStarting: Boolean = false,
    val hasRefreshError: Boolean = false,
    val isStartReady: Boolean = false,
    val suggestedLevel: SpeakingLevel? = null,
    val estimatedDurationMinutes: Int? = null
)
