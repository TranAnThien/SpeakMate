package com.project.speakmate.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.ContentFilterRow
import com.project.speakmate.ui.components.ContentFilterUiState
import com.project.speakmate.ui.components.button.PrimaryButton
import com.project.speakmate.ui.components.button.SecondaryButton
import com.project.speakmate.ui.components.header.ContentPageHeader
import com.project.speakmate.ui.components.lottie.AnimatedMascot
import com.project.speakmate.ui.components.lottie.MascotAnimation
import com.project.speakmate.ui.components.state.SpeakMateLoadingIndicator
import com.project.speakmate.ui.components.state.StatusContent
import com.project.speakmate.ui.components.text.CaptionText
import com.project.speakmate.ui.components.text.DetailText
import com.project.speakmate.ui.history.components.HistoryPendingDiscardDialog
import com.project.speakmate.ui.history.components.HistoryPendingSessionCard
import com.project.speakmate.ui.history.components.HistorySessionCard
import com.project.speakmate.ui.history.state.HistoryGroupUiState
import com.project.speakmate.ui.history.state.HistoryPendingSessionUiState
import com.project.speakmate.ui.history.state.HistoryPresentation
import com.project.speakmate.ui.history.state.HistorySessionUiState
import com.project.speakmate.ui.history.state.HistoryUiState
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.utils.AppConstant

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onSelectFilter: (String) -> Unit,
    onOpenSession: (String) -> Unit,
    onStartPractice: () -> Unit,
    onRetry: () -> Unit,
    onReturnToPath: () -> Unit,
    onRetrySave: (String) -> Unit,
    onSaveUnassessed: (String) -> Unit,
    onRequestDiscard: (String) -> Unit,
    onConfirmDiscard: () -> Unit,
    onCancelDiscard: () -> Unit,
    modifier: Modifier = Modifier,
    onClearFilter: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    listState: LazyListState = rememberLazyListState()
) {
    val allLabel = stringResource(R.string.history_filter_all)
    val lessonsLabel = stringResource(R.string.history_filter_lessons)
    val goalsLabel = stringResource(R.string.history_filter_goal)
    val freeLabel = stringResource(R.string.history_filter_free)
    val filters =
        remember(state.filters, allLabel, lessonsLabel, goalsLabel, freeLabel) {
            state.filters.map { filter ->
                filter.copy(
                    label =
                        when (filter.id) {
                            AppConstant.HISTORY_FILTER_LESSONS -> lessonsLabel
                            AppConstant.HISTORY_FILTER_GOALS -> goalsLabel
                            AppConstant.HISTORY_FILTER_FREE_PRACTICE -> freeLabel
                            else -> allLabel
                        }
                )
            }
        }
    Box(modifier.fillMaxSize().background(DesignColor.Background)) {
        Column(Modifier.fillMaxSize()) {
            ContentPageHeader(stringResource(R.string.navigation_history))
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "history-filters", contentType = ContentFilterUiState::class) {
                    ContentFilterRow(
                        filters = filters,
                        selectedId = state.selectedFilterId,
                        onSelectFilter = onSelectFilter,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                if (state.pendingSessions.isNotEmpty()) {
                    item(key = "history-pending-heading", contentType = "history-pending-heading") {
                        CaptionText(
                            text = stringResource(R.string.history_pending_heading),
                            style = DesignTypography.Label
                        )
                    }
                    items(
                        items = state.pendingSessions,
                        key = { "pending:${it.id}" },
                        contentType = { HistoryPendingSessionUiState::class }
                    ) { session ->
                        val onRetrySession = remember(session.id, onRetrySave) { { onRetrySave(session.id) } }
                        val onSaveSession =
                            remember(session.id, onSaveUnassessed) { { onSaveUnassessed(session.id) } }
                        val onDiscard =
                            remember(session.id, onRequestDiscard) { { onRequestDiscard(session.id) } }
                        val onOpen = remember(session.id, onOpenSession) { { onOpenSession(session.id) } }
                        HistoryPendingSessionCard(
                            session = session,
                            onRetrySave = onRetrySession,
                            onSaveUnassessed = onSaveSession,
                            onDiscard = onDiscard,
                            onOpen = onOpen
                        )
                    }
                }
                item(key = "history-refresh", contentType = "history-refresh") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (state.isRefreshing) SpeakMateLoadingIndicator()
                        if (state.hasRefreshError)
                            DetailText(
                                text = stringResource(R.string.history_refresh_error),
                                color = DesignColor.Error
                            )
                        if (state.presentation == HistoryPresentation.POPULATED || state.hasRefreshError)
                            SecondaryButton(
                                text = stringResource(R.string.content_reload),
                                onClick = onRetry,
                                enabled = !state.isRefreshing
                            )
                    }
                }
                when (state.presentation) {
                    HistoryPresentation.POPULATED ->
                        state.groups.forEach { group ->
                            item(key = "group:${group.id}", contentType = HistoryGroupUiState::class) {
                                val date = rememberHistoryDate(group.startedAtMillis)
                                CaptionText(
                                    text = date,
                                    style = DesignTypography.Label,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            itemsIndexed(
                                items = group.sessions,
                                key = { _, session -> "session:${session.id}" },
                                contentType = { _, _ -> HistorySessionUiState::class }
                            ) { index, session ->
                                val onOpen =
                                    remember(session.id, onOpenSession) { { onOpenSession(session.id) } }
                                HistorySessionCard(
                                    session = session,
                                    onOpenSession = onOpen,
                                    modifier =
                                        Modifier.padding(
                                            bottom = if (index == group.sessions.lastIndex) 8.dp else 0.dp
                                        )
                                )
                            }
                        }
                    HistoryPresentation.LOADING ->
                        item(key = "history-loading", contentType = HistoryPresentation.LOADING) {
                            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                DetailText(
                                    text = stringResource(R.string.history_loading),
                                    color = DesignColor.Primary
                                )
                                SpeakMateLoadingIndicator()
                            }
                        }
                    HistoryPresentation.EMPTY,
                    HistoryPresentation.NO_MATCH,
                    HistoryPresentation.LOAD_ERROR ->
                        if (state.presentation != HistoryPresentation.EMPTY || state.pendingSessions.isEmpty())
                            item(key = "history-status", contentType = state.presentation) {
                                StatusContent(
                                    title =
                                        stringResource(
                                            when (state.presentation) {
                                                HistoryPresentation.NO_MATCH -> R.string.history_no_match_title
                                                HistoryPresentation.EMPTY -> R.string.history_empty_title
                                                else -> R.string.history_error_title
                                            }
                                        ),
                                    titleStyle = DesignTypography.Heading,
                                    titleColor = DesignColor.Text,
                                    description =
                                        stringResource(
                                            when (state.presentation) {
                                                HistoryPresentation.NO_MATCH ->
                                                    R.string.history_no_match_description
                                                HistoryPresentation.EMPTY -> R.string.history_empty_description
                                                else -> R.string.history_error_description
                                            }
                                        ),
                                    descriptionStyle = DesignTypography.Body,
                                    descriptionColor = DesignColor.Muted,
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    illustration = {
                                        AnimatedMascot(
                                            animation =
                                                if (state.presentation == HistoryPresentation.LOAD_ERROR)
                                                    MascotAnimation.NETWORK_ERROR
                                                else MascotAnimation.EMPTY_STATE,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(120.dp)
                                                .padding(12.dp)
                                        )
                                    },
                                    actions = {
                                        PrimaryButton(
                                            stringResource(
                                                when (state.presentation) {
                                                    HistoryPresentation.NO_MATCH -> R.string.history_clear_filter
                                                    HistoryPresentation.EMPTY -> R.string.history_start_practice
                                                    else -> R.string.content_reload
                                                }
                                            ),
                                            when (state.presentation) {
                                                HistoryPresentation.NO_MATCH -> onClearFilter
                                                HistoryPresentation.EMPTY -> onStartPractice
                                                else -> onRetry
                                            }
                                        )
                                        if (state.presentation == HistoryPresentation.LOAD_ERROR)
                                            SecondaryButton(
                                                stringResource(R.string.goal_assessment_return_to_path),
                                                onReturnToPath
                                            )
                                    }
                                )
                            }
                }
                if (state.isLoadingMore || state.hasLoadMoreError) {
                    item(key = "history-more-status", contentType = "history-more-status") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (state.isLoadingMore) SpeakMateLoadingIndicator()
                            if (state.hasLoadMoreError) {
                                DetailText(
                                    text = stringResource(R.string.history_load_more_error),
                                    color = DesignColor.Error
                                )
                                SecondaryButton(stringResource(R.string.content_retry), onLoadMore)
                            }
                        }
                    }
                }
            }
        }

        if (state.pendingDiscardId != null) HistoryPendingDiscardDialog(onConfirmDiscard, onCancelDiscard)
    }
}
