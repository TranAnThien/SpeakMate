package com.project.speakmate.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.project.speakmate.R
import com.project.speakmate.ui.components.ContentFilterUiState
import com.project.speakmate.ui.history.state.HistoryGroupUiState
import com.project.speakmate.ui.history.state.HistoryPresentation
import com.project.speakmate.ui.history.state.HistoryResultPresentation
import com.project.speakmate.ui.history.state.HistorySessionUiState
import com.project.speakmate.ui.history.state.HistoryUiState
import com.project.speakmate.ui.theme.SpeakMateTheme

@Preview(name = "History / Source / 17:701", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun HistoryPopulatedScreenPreview() {
    SpeakMateTheme {
        HistoryScreen(
            state = rememberHistoryPreviewState(HistoryPresentation.POPULATED),
            onSelectFilter = {},
            onOpenSession = {},
            onStartPractice = {},
            onRetry = {},
            onDismissError = {},
            onOpenTrainingPath = {},
            onOpenPractice = {},
            onOpenHistory = {},
            onOpenProfile = {}
        )
    }
}

@Preview(name = "History / Empty / Added (SRS) / 32:2113", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun HistoryEmptyScreenPreview() {
    SpeakMateTheme {
        HistoryScreen(
            state = rememberHistoryPreviewState(HistoryPresentation.EMPTY),
            onSelectFilter = {},
            onOpenSession = {},
            onStartPractice = {},
            onRetry = {},
            onDismissError = {},
            onOpenTrainingPath = {},
            onOpenPractice = {},
            onOpenHistory = {},
            onOpenProfile = {}
        )
    }
}

@Preview(name = "UX State Additions • History • Loading • Proposal / 33:2486", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun HistoryLoadingScreenPreview() {
    SpeakMateTheme {
        HistoryScreen(
            state = rememberHistoryPreviewState(HistoryPresentation.LOADING),
            onSelectFilter = {},
            onOpenSession = {},
            onStartPractice = {},
            onRetry = {},
            onDismissError = {},
            onOpenTrainingPath = {},
            onOpenPractice = {},
            onOpenHistory = {},
            onOpenProfile = {}
        )
    }
}

@Preview(name = "History / Load Error / Retry / Audited / 33:2501", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun HistoryLoadErrorScreenPreview() {
    SpeakMateTheme {
        HistoryScreen(
            state = rememberHistoryPreviewState(HistoryPresentation.LOAD_ERROR),
            onSelectFilter = {},
            onOpenSession = {},
            onStartPractice = {},
            onRetry = {},
            onDismissError = {},
            onOpenTrainingPath = {},
            onOpenPractice = {},
            onOpenHistory = {},
            onOpenProfile = {}
        )
    }
}

@Preview(name = "History / Load Error / Dialog / Audited / 59:1185", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun HistoryErrorDialogScreenPreview() {
    SpeakMateTheme {
        HistoryScreen(
            state = rememberHistoryPreviewState(HistoryPresentation.ERROR_DIALOG),
            onSelectFilter = {},
            onOpenSession = {},
            onStartPractice = {},
            onRetry = {},
            onDismissError = {},
            onOpenTrainingPath = {},
            onOpenPractice = {},
            onOpenHistory = {},
            onOpenProfile = {}
        )
    }
}

@Composable
private fun rememberHistoryPreviewState(presentation: HistoryPresentation): HistoryUiState = HistoryUiState(
    presentation = presentation,
    filters = listOf(
        ContentFilterUiState("all", stringResource(R.string.content_filter_all)),
        ContentFilterUiState("unit", stringResource(R.string.history_filter_unit)),
        ContentFilterUiState("goal", stringResource(R.string.history_filter_goal)),
        ContentFilterUiState("free", stringResource(R.string.history_filter_free))
    ),
    selectedFilterId = "all",
    groups = if (presentation == HistoryPresentation.POPULATED) listOf(
        HistoryGroupUiState("today", stringResource(R.string.history_today), listOf(
            HistorySessionUiState("passed", stringResource(R.string.history_preview_passed_metadata),
                stringResource(R.string.history_preview_passed_title), stringResource(R.string.history_preview_passed_result),
                HistoryResultPresentation.PASSED),
            HistorySessionUiState("partial", stringResource(R.string.history_preview_partial_metadata),
                stringResource(R.string.history_preview_partial_title), stringResource(R.string.history_preview_partial_result),
                HistoryResultPresentation.PARTIAL),
            HistorySessionUiState("failed", stringResource(R.string.history_preview_failed_metadata),
                stringResource(R.string.history_preview_failed_title), stringResource(R.string.history_preview_failed_result),
                HistoryResultPresentation.NOT_PASSED)
        )),
        HistoryGroupUiState("yesterday", stringResource(R.string.history_yesterday), listOf(
            HistorySessionUiState("free", stringResource(R.string.history_preview_free_metadata),
                stringResource(R.string.history_preview_free_title), stringResource(R.string.history_preview_free_result),
                HistoryResultPresentation.COMPLETED)
        ))
    ) else emptyList()
)
