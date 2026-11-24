package com.project.speakmate.ui.profile.state

import com.project.speakmate.ui.components.dialog.LegalDocument

sealed interface SettingsDialog {
    data object LanguageSelection : SettingsDialog

    data object SignOutConfirmation : SettingsDialog

    data class Notice(val document: LegalDocument) : SettingsDialog
}
