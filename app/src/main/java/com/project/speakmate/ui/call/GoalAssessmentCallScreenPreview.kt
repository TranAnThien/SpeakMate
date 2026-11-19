package com.project.speakmate.ui.call

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.project.speakmate.ui.call.components.CallDurationLabel
import com.project.speakmate.ui.call.state.CallPresentation
import com.project.speakmate.ui.call.state.CallUiState
import com.project.speakmate.ui.theme.SpeakMateTheme

@Preview(name = "Goal Assessment / Listening / No Transcript / 104:387", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun GoalAssessmentCallListeningScreenPreview() {
    SpeakMateTheme {
        CallScreen(
            state = CallUiState(
                presentation = CallPresentation.LISTENING,
                title = "Đánh giá: Tại quán cà phê",
                isTranscriptAvailable = false
            ),
            durationContent = { CallDurationLabel("02:18") },
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
