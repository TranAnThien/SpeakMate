package com.project.speakmate.ui.auth.state

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class GoogleSignInUiState(
    val status: GoogleSignInStatus = GoogleSignInStatus.NORMAL
)
