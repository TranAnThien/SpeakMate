package com.project.speakmate.ui.auth.state

import androidx.compose.runtime.Immutable
import com.project.speakmate.ui.components.LegalDocument

@Immutable
data class GoogleSignInUiState(
    val status: GoogleSignInStatus = GoogleSignInStatus.NORMAL,
    val userId: String? = null,
    val isCheckingProfile: Boolean = true,
    val isProfileReady: Boolean = false,
    val legalDocument: LegalDocument? = null
)
