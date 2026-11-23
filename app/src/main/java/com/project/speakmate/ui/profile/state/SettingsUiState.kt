package com.project.speakmate.ui.profile.state

import androidx.compose.runtime.Immutable
import com.project.speakmate.domain.model.AppLanguage
import com.project.speakmate.ui.components.LegalDocument

@Immutable
data class SettingsUiState(
    val isSignOutConfirmationVisible: Boolean = false,
    val isSigningOut: Boolean = false,
    val isSignOutFailed: Boolean = false,
    val pendingCompletionCount: Int? = null,
    val notice: LegalDocument? = null,
    val selectedLanguage: AppLanguage = AppLanguage.VIETNAMESE,
    val isLanguageSelectionVisible: Boolean = false,
    val isLanguageLoading: Boolean = true,
    val isLanguageSaving: Boolean = false,
    val isLanguageSaveFailed: Boolean = false,
    val hasLanguageLoadError: Boolean = false
)
