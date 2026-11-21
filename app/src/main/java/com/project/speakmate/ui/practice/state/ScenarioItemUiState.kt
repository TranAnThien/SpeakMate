package com.project.speakmate.ui.practice.state

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable

@Immutable
data class ScenarioItemUiState(
    val id: String = "",
    val title: String = "",
    val categoryLabel: String = "",
    val roleLabel: String? = "",
    val description: String = "",
    val imageUrl: String? = null,
    @param:DrawableRes val imageRes: Int? = null
)
