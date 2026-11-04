package com.project.speakmate.ui.practice.state

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class ScenarioPreparationUiState(
    val title: String = "",
    @param:DrawableRes val coverRes: Int? = null,
    val context: String = "",
    val aiRole: String = "",
    val hint: String = "",
    val isInitializing: Boolean = false
)
