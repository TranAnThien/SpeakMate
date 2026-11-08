package com.project.speakmate.ui.practice.state

import androidx.annotation.DrawableRes
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
    @param:DrawableRes val coverRes: Int? = null,
    val context: String = "",
    val aiRole: String = "",
    val hint: String = "",
    val scenario: Scenario? = null,
    val isInitializing: Boolean = false
)
