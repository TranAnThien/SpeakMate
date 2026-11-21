package com.project.speakmate.ui.auth.state

sealed interface AuthEffect {
    data object SignedIn : AuthEffect
}
