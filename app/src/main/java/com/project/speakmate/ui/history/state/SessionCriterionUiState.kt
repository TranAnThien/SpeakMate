package com.project.speakmate.ui.history.state

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class SessionCriterionUiState(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val isPassed: Boolean = false
)
