package com.project.speakmate.ui.placement.state

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class PlacementStatusUiState(
    val status: PlacementStatus = PlacementStatus.PROCESSING,
    val isTimeLimitReached: Boolean = false
)
