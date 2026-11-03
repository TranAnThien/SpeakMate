package com.project.speakmate.ui.call.state

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class MicrophonePermissionUiState(
    val presentation: MicrophonePermissionPresentation = MicrophonePermissionPresentation.RATIONALE
)
