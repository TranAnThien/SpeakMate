package com.project.speakmate.ui.practice.state

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class ScenarioItemUiState(
    val id: String = "",
    val title: String = "",
    val categoryLabel: String = "",
    val roleLabel: String = "",
    val description: String = "",
    @param:DrawableRes val imageRes: Int? = null
)
