package com.project.speakmate.ui.placement.state

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class PromotionResultUiState(
    val presentation: PromotionResultPresentation = PromotionResultPresentation.PROMOTED,
    val title: String = "",
    val description: String = "",
    val currentLevelLabel: String = "",
    val nextLevelLabel: String = ""
)
