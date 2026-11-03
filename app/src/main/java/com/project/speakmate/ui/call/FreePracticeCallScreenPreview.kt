package com.project.speakmate.ui.call

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.project.speakmate.R
import com.project.speakmate.ui.call.state.CallPresentation
import com.project.speakmate.ui.call.state.CallUiState
import com.project.speakmate.ui.theme.SpeakMateTheme

@Preview(name = "Free Practice / Listening / Transcript available / 105:629", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun FreePracticeCallListeningScreenPreview() {
    SpeakMateTheme {
        CallScreen(
            state = CallUiState(
                presentation = CallPresentation.LISTENING,
                title = stringResource(R.string.practice_preview_cafe_title),
                durationLabel = "02:18"
            ),
            onStartCall = {},
            onReturnToPreparation = {},
            onToggleMute = {},
            onEndCall = {},
            onConfirmEndCall = {},
            onContinueCall = {},
            onRetry = {},
            onExit = {},
            onOpenTranscript = {},
            onCloseTranscript = {},
            onSelectTranscriptWord = { _, _, _ -> },
            onCloseDictionary = {},
            onPlayDictionaryWord = {},
            onSaveDictionaryWord = {}
        )
    }
}
