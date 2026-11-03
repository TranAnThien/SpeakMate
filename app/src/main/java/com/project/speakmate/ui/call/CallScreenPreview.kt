package com.project.speakmate.ui.call

import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.speakmate.ui.call.state.CallDictionaryUiState
import com.project.speakmate.ui.call.state.CallPresentation
import com.project.speakmate.ui.call.state.CallTranscriptMessageUiState
import com.project.speakmate.ui.call.state.CallUiState
import com.project.speakmate.ui.theme.SpeakMateTheme

@Preview(name = "Voice Call / Ready / Pre-active / 17:238", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun CallReadyScreenPreview() {
    SpeakMateTheme {
        CallScreen(
            state = getCallPreviewState(CallPresentation.READY),
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

@Preview(name = "Voice Call / Connecting / Timer stopped / 33:2437", widthDp = 412, heightDp = 851, showBackground = true)
@Composable
private fun CallConnectingScreenPreview() {
    SpeakMateTheme {
        CallScreen(
            state = getCallPreviewState(CallPresentation.CONNECTING),
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

@Preview(name = "Voice Call / Listening / 17:239", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun CallListeningScreenPreview() {
    SpeakMateTheme {
        CallScreen(
            state = getCallPreviewState(CallPresentation.LISTENING),
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

@Preview(name = "Voice Call / Processing / 17:240", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun CallProcessingScreenPreview() {
    SpeakMateTheme {
        CallScreen(
            state = getCallPreviewState(CallPresentation.PROCESSING),
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

@Preview(name = "Voice Call / Speaking / 17:241", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun CallSpeakingScreenPreview() {
    SpeakMateTheme {
        CallScreen(
            state = getCallPreviewState(CallPresentation.SPEAKING),
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

@Preview(name = "Voice Call / Muted / 17:242", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun CallMutedScreenPreview() {
    SpeakMateTheme {
        CallScreen(
            state = getCallPreviewState(CallPresentation.MUTED),
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

@Preview(name = "Voice Call / Barge-in / 36:4373", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun CallBargeInScreenPreview() {
    SpeakMateTheme {
        CallScreen(
            state = getCallPreviewState(CallPresentation.BARGE_IN),
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

@Preview(name = "Voice Call / Audio unclear / Active / 17:243", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun CallAudioUnclearScreenPreview() {
    SpeakMateTheme {
        CallScreen(
            state = getCallPreviewState(CallPresentation.AUDIO_UNCLEAR),
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

@Preview(name = "Voice Call / Failed / Retry or exit / 17:374", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun CallFailedScreenPreview() {
    SpeakMateTheme {
        CallScreen(
            state = getCallPreviewState(CallPresentation.FAILED),
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

@Preview(name = "Voice Call / End-call confirmation / 33:2646", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun CallEndConfirmationScreenPreview() {
    SpeakMateTheme {
        CallScreen(
            state = getCallPreviewState(CallPresentation.LISTENING).copy(isEndConfirmationVisible = true),
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

@Preview(name = "Voice Call / Live transcript / Top / 104:417", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun CallTranscriptScreenPreview() {
    SpeakMateTheme {
        CallScreen(
            state = getCallPreviewState(CallPresentation.LISTENING).copy(isTranscriptVisible = true),
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

@Preview(name = "Voice Call / Live transcript / Bottom / 104:485", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun CallTranscriptBottomScreenPreview() {
    SpeakMateTheme {
        CallScreen(
            state = getCallPreviewState(CallPresentation.LISTENING).copy(isTranscriptVisible = true),
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
            onSaveDictionaryWord = {},
            transcriptScrollState = rememberScrollState(initial = with(LocalDensity.current) { 105.dp.roundToPx() })
        )
    }
}

@Preview(name = "Voice Call / Dictionary / Success / large / 105:551", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun CallDictionaryScreenPreview() {
    SpeakMateTheme {
        CallScreen(
            state = getCallPreviewState(CallPresentation.LISTENING).copy(isTranscriptVisible = true, dictionary = getCallDictionaryPreviewState()),
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
            onSaveDictionaryWord = {},
            transcriptScrollState = rememberScrollState(initial = with(LocalDensity.current) { 80.dp.roundToPx() })
        )
    }
}

private fun getCallPreviewState(presentation: CallPresentation): CallUiState = CallUiState(
    presentation = presentation,
    title = "Điều chỉnh đơn gọi món",
    durationLabel = when (presentation) {
        CallPresentation.READY -> "00:00"
        CallPresentation.CONNECTING -> ""
        else -> "02:18"
    },
    messages = listOf(
        CallTranscriptMessageUiState("turn_1", "Hello! How can I help you with your order?", false),
        CallTranscriptMessageUiState("turn_2", "Could I change my iced latte to a large?", true),
        CallTranscriptMessageUiState("turn_3", "Of course! Would you like the same amount of sugar?", false),
        CallTranscriptMessageUiState("turn_4", "Could you make it with less sugar, please?", true),
        CallTranscriptMessageUiState("turn_5", "Certainly! A large iced latte with less sugar. Is that correct?", false),
        CallTranscriptMessageUiState("turn_6", "Yes, that is correct. Thank you!", true)
    )
)

private fun getCallDictionaryPreviewState(): CallDictionaryUiState = CallDictionaryUiState(
    word = "large",
    pronunciationLabel = "/lɑːrdʒ/ · tính từ",
    meaning = "cỡ lớn",
    exampleSentence = "Could I change my iced latte to a large?",
    exampleTranslation = "Tôi có thể đổi latte đá sang cỡ lớn không?",
    messageId = "turn_2",
    selectionStart = "Could I change my iced latte to a large?".indexOf("large"),
    anchorTopDp = 216f
)

