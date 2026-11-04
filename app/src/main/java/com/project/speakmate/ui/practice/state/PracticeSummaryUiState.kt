package com.project.speakmate.ui.practice.state

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class PracticeSummaryUiState(
    val scenarioTitle: String = "",
    val sessionDescription: String = ""
)
