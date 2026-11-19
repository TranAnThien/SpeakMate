package com.project.speakmate.ui.auth.state

import androidx.compose.runtime.Immutable

@Immutable
data class GoogleSignInUiState(
    val status: GoogleSignInStatus = GoogleSignInStatus.NORMAL
)
