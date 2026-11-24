package com.project.speakmate.ui.profile.state

import androidx.compose.runtime.Immutable
import com.project.speakmate.domain.model.AppLanguage

@Immutable
data class SettingsUiState(
    val activeDialog: SettingsDialog? = null,
    val isSigningOut: Boolean = false,
    val isSignOutFailed: Boolean = false,
    val pendingCompletionCount: Int? = null,
    val selectedLanguage: AppLanguage = AppLanguage.VIETNAMESE,
    val isLanguageLoading: Boolean = true,
    val isLanguageSaving: Boolean = false,
    val isLanguageSaveFailed: Boolean = false,
    val hasLanguageLoadError: Boolean = false,
    val isRecreateRequired: Boolean = false
) {
    val isSignOutConfirmationVisible: Boolean
        get() = activeDialog == SettingsDialog.SignOutConfirmation

    val isLanguageSelectionVisible: Boolean
        get() = activeDialog == SettingsDialog.LanguageSelection
}
