package com.project.speakmate.ui.practice.state

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import com.project.speakmate.domain.model.Scenario

@Keep
@Immutable
data class ScenarioPreparationUiState(
    val presentation: ScenarioPreparationPresentation = ScenarioPreparationPresentation.LOADING,
    val errorMessage: String = "",
    val practiceId: String = "",
    val scenarioId: String = "",
    val title: String = "",
    val description: String = "",
    /** Retains the Scenario for call setup; promptTemplate is never rendered by this screen. */
    val scenario: Scenario? = null,
    val isInitializing: Boolean = false
)
