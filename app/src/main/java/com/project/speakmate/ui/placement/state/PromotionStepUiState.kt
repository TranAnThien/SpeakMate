package com.project.speakmate.ui.placement.state

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class PromotionStepUiState(
    val id: String = "",
    val number: Int = 1,
    val text: String = ""
) {
    val numberLabel: String
        get() = number.toString()
}
