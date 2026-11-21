package com.project.speakmate.ui.profile.state

sealed interface ProfileEffect {
    data object SignedOut : ProfileEffect
}
