package com.project.speakmate.ui.call

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.project.speakmate.R
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
                title = stringResource(R.string.goal_assessment_preview_call_title),
                durationLabel = "02:18",
                isTranscriptAvailable = false
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
