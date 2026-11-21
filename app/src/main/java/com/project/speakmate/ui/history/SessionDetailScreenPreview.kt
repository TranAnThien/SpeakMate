package com.project.speakmate.ui.history

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.history.state.SessionActionUiState
import com.project.speakmate.ui.history.state.SessionCriterionUiState
import com.project.speakmate.ui.history.state.SessionDetailPresentation
import com.project.speakmate.ui.history.state.SessionDetailUiState
import com.project.speakmate.ui.history.state.SessionMessageUiState
import com.project.speakmate.ui.theme.SpeakMateTheme

@Preview(name = "Session Detail / Default / Audited / 17:702", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun SessionDetailCompletedScreenPreview() {
    SpeakMateTheme {
        SessionDetailScreen(
            state = rememberSessionDetailPreviewState(SessionDetailPresentation.COMPLETED),
            onBack = {},
            onRetrySession = {},
            onRetryTranscript = {},
            onDismissError = {},
            onReturnToHistory = {}
        )
    }
}

@Preview(name = "Session Detail / Completed / Scroll / Bottom / 131:6223", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun SessionDetailCompletedBottomScreenPreview() {
    SpeakMateTheme {
        SessionDetailScreen(
            state = rememberSessionDetailPreviewState(SessionDetailPresentation.COMPLETED),
            onBack = {},
            onRetrySession = {},
            onRetryTranscript = {},
            onDismissError = {},
            onReturnToHistory = {},
            listState = rememberLazyListState(initialFirstVisibleItemScrollOffset = with(LocalDensity.current) { 709.dp.roundToPx() })
        )
    }
}

@Preview(name = "Session Detail / Transcript Load Error / Default / Audited / 33:2516", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun SessionDetailTranscriptErrorScreenPreview() {
    SpeakMateTheme {
        SessionDetailScreen(
            state = rememberSessionDetailPreviewState(SessionDetailPresentation.TRANSCRIPT_ERROR),
            onBack = {},
            onRetrySession = {},
            onRetryTranscript = {},
            onDismissError = {},
            onReturnToHistory = {}
        )
    }
}

@Preview(name = "Session Detail / Transcript Load Error / Scroll / Bottom / 131:6405", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun SessionDetailTranscriptErrorBottomScreenPreview() {
    SpeakMateTheme {
        SessionDetailScreen(
            state = rememberSessionDetailPreviewState(SessionDetailPresentation.TRANSCRIPT_ERROR),
            onBack = {},
            onRetrySession = {},
            onRetryTranscript = {},
            onDismissError = {},
            onReturnToHistory = {},
            listState = rememberLazyListState(initialFirstVisibleItemScrollOffset = with(LocalDensity.current) { 365.dp.roundToPx() })
        )
    }
}

@Preview(name = "History / Session Detail / Loading / Added • UX gap P1 / 36:4189", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun SessionDetailLoadingScreenPreview() {
    SpeakMateTheme {
        SessionDetailScreen(
            state = rememberSessionDetailPreviewState(SessionDetailPresentation.LOADING),
            onBack = {},
            onRetrySession = {},
            onRetryTranscript = {},
            onDismissError = {},
            onReturnToHistory = {}
        )
    }
}

@Preview(name = "Session Detail / Load Error / Retry / Audited / 36:4269", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun SessionDetailLoadErrorScreenPreview() {
    SpeakMateTheme {
        SessionDetailScreen(
            state = rememberSessionDetailPreviewState(SessionDetailPresentation.LOAD_ERROR),
            onBack = {},
            onRetrySession = {},
            onRetryTranscript = {},
            onDismissError = {},
            onReturnToHistory = {}
        )
    }
}

@Preview(name = "Session Detail / Load Error / Dialog / Audited / 59:1416", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun SessionDetailErrorDialogScreenPreview() {
    SpeakMateTheme {
        SessionDetailScreen(
            state = rememberSessionDetailPreviewState(SessionDetailPresentation.ERROR_DIALOG),
            onBack = {},
            onRetrySession = {},
            onRetryTranscript = {},
            onDismissError = {},
            onReturnToHistory = {}
        )
    }
}

@Composable
private fun rememberSessionDetailPreviewState(presentation: SessionDetailPresentation): SessionDetailUiState = SessionDetailUiState(
    presentation = presentation,
    title = "Điều chỉnh đơn gọi món",
    metadata = if (presentation == SessionDetailPresentation.TRANSCRIPT_ERROR) "Unit · Hôm nay, 14:20 · 05:16 · Đạt\nChưa tải được bản ghi hội thoại." else "Unit · Hôm nay, 14:20 · 05:16 · Đạt",
    criteriaHeading = stringResource(R.string.unit_result_pass_criteria_heading),
    requiredActions = listOf(
        SessionActionUiState("resize", "Yêu cầu đổi kích cỡ đồ uống.", true),
        SessionActionUiState("confirm", "Xác nhận lại đơn sau khi điều chỉnh.", true)
    ),
    criteria = listOf(
        SessionCriterionUiState("range", stringResource(R.string.unit_result_criterion_name_0),
            stringResource(R.string.unit_result_criterion_description_0), true),
        SessionCriterionUiState("accuracy", stringResource(R.string.unit_result_criterion_name_1),
            stringResource(R.string.unit_result_criterion_description_1), false),
        SessionCriterionUiState("fluency", stringResource(R.string.unit_result_criterion_name_2),
            stringResource(R.string.unit_result_criterion_description_2), true),
        SessionCriterionUiState("interaction", stringResource(R.string.unit_result_criterion_name_3),
            stringResource(R.string.unit_result_criterion_description_3), true),
        SessionCriterionUiState("coherence", stringResource(R.string.unit_result_criterion_name_4),
            stringResource(R.string.unit_result_criterion_description_4), true)
    ),
    messages = if (presentation == SessionDetailPresentation.COMPLETED) listOf(
        SessionMessageUiState("message_0", "AI · 00:18",
            "What would you like to order?", false),
        SessionMessageUiState("message_1", "BẠN · 00:32",
            "I'd like an iced latte, please.", true),
        SessionMessageUiState("message_2", "AI · 00:41",
            "What size would you like?", false),
        SessionMessageUiState("message_3", "BẠN · 00:54",
            "A medium, please. Could you make it with less sugar?", true),
        SessionMessageUiState("message_4", "BẠN · 01:12",
            "Actually, could you change it to a large?", true),
        SessionMessageUiState("message_5", "AI · 01:20",
            "Of course. A large iced latte with less sugar.", false),
        SessionMessageUiState("message_6", "BẠN · 01:31",
            "Yes, that's correct. Thank you.", true)
    ) else emptyList()
)
