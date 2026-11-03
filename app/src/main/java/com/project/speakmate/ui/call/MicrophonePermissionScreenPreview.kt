package com.project.speakmate.ui.call

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.project.speakmate.ui.call.state.MicrophonePermissionPresentation
import com.project.speakmate.ui.call.state.MicrophonePermissionUiState
import com.project.speakmate.ui.theme.SpeakMateTheme

@Preview(name = "Microphone Permission / Rationale / 17:375", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun MicrophoneRationaleScreenPreview() {
    SpeakMateTheme {
        MicrophonePermissionScreen(
            state = MicrophonePermissionUiState(MicrophonePermissionPresentation.RATIONALE),
            onRequestPermission = {},
            onOpenSettings = {},
            onRecheckPermission = {},
            onBack = {},
            onExit = {}
        )
    }
}

@Preview(name = "Microphone Permission / Denied / Open settings / 33:2452", widthDp = 412, heightDp = 851, showBackground = true)
@Composable
private fun MicrophoneDeniedScreenPreview() {
    SpeakMateTheme {
        MicrophonePermissionScreen(
            state = MicrophonePermissionUiState(MicrophonePermissionPresentation.DENIED),
            onRequestPermission = {},
            onOpenSettings = {},
            onRecheckPermission = {},
            onBack = {},
            onExit = {}
        )
    }
}

@Preview(name = "Microphone Permission / Revoked / Recovery / 36:3836", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun MicrophoneRevokedScreenPreview() {
    SpeakMateTheme {
        MicrophonePermissionScreen(
            state = MicrophonePermissionUiState(MicrophonePermissionPresentation.REVOKED),
            onRequestPermission = {},
            onOpenSettings = {},
            onRecheckPermission = {},
            onBack = {},
            onExit = {}
        )
    }
}


