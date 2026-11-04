package com.project.speakmate.ui.placement.state

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class PromotionIntroductionUiState(
    val title: String = "",
    val description: String = "",
    val steps: List<PromotionStepUiState> = emptyList(),
    val conditionTitle: String = "",
    val conditionDescription: String = "",
    val note: String = ""
)
