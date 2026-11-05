package com.project.speakmate.ui.profile.state

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class SettingsUiState(
    val languageLabel: String = "",
    val isSignOutConfirmationVisible: Boolean = false
)
