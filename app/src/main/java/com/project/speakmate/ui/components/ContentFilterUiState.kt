package com.project.speakmate.ui.components

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class ContentFilterUiState(
    val id: String = "",
    val label: String = ""
)
