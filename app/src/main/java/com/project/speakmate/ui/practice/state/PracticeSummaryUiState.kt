package com.project.speakmate.ui.practice.state

import androidx.compose.runtime.Immutable

@Immutable
data class PracticeSummaryUiState(
    val scenarioTitle: String = "",
    val sessionDescription: String = ""
)
