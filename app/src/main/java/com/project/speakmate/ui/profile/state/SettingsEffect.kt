package com.project.speakmate.ui.profile.state



sealed interface SettingsEffect {
    data object RecreateActivity : SettingsEffect
}
