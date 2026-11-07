package com.project.speakmate.ui.call.state

import androidx.compose.runtime.Immutable

@Immutable
data class MicrophonePermissionUiState(
    val presentation: MicrophonePermissionPresentation = MicrophonePermissionPresentation.RATIONALE
)
