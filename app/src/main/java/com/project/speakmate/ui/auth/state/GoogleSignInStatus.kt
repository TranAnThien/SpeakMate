package com.project.speakmate.ui.auth.state

enum class GoogleSignInStatus {
    NORMAL,
    LOADING,
    ERROR,
    CANCELLED,
    OFFLINE,
    PROFILE_ERROR,
    SIGNING_OUT
}
