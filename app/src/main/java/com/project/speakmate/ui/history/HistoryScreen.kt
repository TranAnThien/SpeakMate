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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.AnimatedMascot
import com.project.speakmate.ui.components.ContentFilterRow
import com.project.speakmate.ui.components.ContentFilterUiState
import com.project.speakmate.ui.components.ContentPageHeader
import com.project.speakmate.ui.components.PrimaryButton
import com.project.speakmate.ui.components.SecondaryButton
import com.project.speakmate.ui.components.SpeakMateLoadingIndicator
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
    onDismissError: () -> Unit,
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
    val locale = LocalConfiguration.current.locales[0]
    Box(modifier.fillMaxSize().background(DesignColor.Background)) {
        Column(Modifier.fillMaxSize()) {
            ContentPageHeader(stringResource(R.string.navigation_history))
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item(key = "history-filters", contentType = ContentFilterUiState::class) {
                    ContentFilterRow(state.filters.map { filter ->
                        filter.copy(label = stringResource(when (filter.id) {
                            AppConstant.HISTORY_FILTER_LESSONS -> R.string.history_filter_lessons
                            AppConstant.HISTORY_FILTER_GOALS -> R.string.history_filter_goal
                            AppConstant.HISTORY_FILTER_FREE_PRACTICE -> R.string.history_filter_free
                            else -> R.string.history_filter_all
                        }))
                    }, state.selectedFilterId, onSelectFilter, Modifier.padding(bottom = 8.dp))
                }
                if (state.pendingSessions.isNotEmpty()) {
                    item(key = "history-pending-heading", contentType = HistoryPendingSessionUiState::class) {
                        Text(stringResource(R.string.history_pending_heading), style = DesignTypography.Label, color = DesignColor.Muted)
                    }
                    items(state.pendingSessions.distinctBy { it.id }, key = { "pending:${it.id}" }, contentType = { HistoryPendingSessionUiState::class }) { session ->
                        val onRetrySession = remember(session.id, onRetrySave) { { onRetrySave(session.id) } }
                        val onSaveSession = remember(session.id, onSaveUnassessed) { { onSaveUnassessed(session.id) } }
                        val onDiscard = remember(session.id, onRequestDiscard) { { onRequestDiscard(session.id) } }
                        val onOpen = remember(session.id, onOpenSession) { { onOpenSession(session.id) } }
                        HistoryPendingSessionCard(session, onRetrySession, onSaveSession, onDiscard, onOpen = onOpen)
                    }
                }
                item(key = "history-refresh", contentType = HistoryUiState::class) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (state.isRefreshing) SpeakMateLoadingIndicator()
                        if (state.hasRefreshError) Text(stringResource(R.string.history_refresh_error), style = DesignTypography.Detail, color = DesignColor.Error)
                        if (state.presentation == HistoryPresentation.POPULATED ||
                            state.hasRefreshError) SecondaryButton(stringResource(R.string.content_reload), onRetry, enabled = !state.isRefreshing)
                    }
                }
                when (state.presentation) {
                    HistoryPresentation.POPULATED -> state.groups.distinctBy { it.id }.forEach { group ->
                        item(key = "group:${group.id}", contentType = HistoryGroupUiState::class) {
                            val date = rememberHistoryDate(group.startedAtMillis)
                            Text(date, style = DesignTypography.Label, color = DesignColor.Muted, modifier = Modifier.padding(bottom = 8.dp))
                        }
                        itemsIndexed(group.sessions.distinctBy { it.id },
                            key = { _, session -> "session:${session.id}" },
                            contentType = { _, _ -> HistorySessionUiState::class }) { index, session ->
                            val onOpen = remember(session.id, onOpenSession) { { onOpenSession(session.id) } }
                            HistorySessionCard(session, onOpen, Modifier.padding(bottom = if (index == group.sessions.lastIndex) 8.dp else 0.dp))
                        }
                    }
                    HistoryPresentation.LOADING -> item(key = "history-loading", contentType = HistoryPresentation.LOADING) {
                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            Text(stringResource(R.string.history_loading), style = DesignTypography.Detail, color = DesignColor.Primary)
                            SpeakMateLoadingIndicator()
                        }
                    }
                    HistoryPresentation.EMPTY, HistoryPresentation.NO_MATCH, HistoryPresentation.LOAD_ERROR ->
                        if (state.presentation != HistoryPresentation.EMPTY || state.pendingSessions.isEmpty()) item(key = "history-status", contentType = state.presentation) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            AnimatedMascot(
                                if (state.presentation == HistoryPresentation.LOAD_ERROR) R.raw.lottie_network_error else R.raw.lottie_empty_state,
                                if (state.presentation == HistoryPresentation.LOAD_ERROR) R.drawable.img_call_error else R.drawable.img_placement_mascot,
                                Modifier.fillMaxWidth().height(120.dp).padding(12.dp))
                            Text(stringResource(when (state.presentation) {
                                HistoryPresentation.NO_MATCH -> R.string.history_no_match_title
                                HistoryPresentation.EMPTY -> R.string.history_empty_title
                                else -> R.string.history_error_title
                            }), style = DesignTypography.Heading, color = DesignColor.Text)
                            Text(stringResource(when (state.presentation) {
                                HistoryPresentation.NO_MATCH -> R.string.history_no_match_description
                                HistoryPresentation.EMPTY -> R.string.history_empty_description
                                else -> R.string.history_error_description
                            }), style = DesignTypography.Body, color = DesignColor.Muted)
                            PrimaryButton(stringResource(when (state.presentation) {
                                HistoryPresentation.NO_MATCH -> R.string.history_clear_filter
                                HistoryPresentation.EMPTY -> R.string.history_start_practice
                                else -> R.string.content_reload
                            }), when (state.presentation) {
                                HistoryPresentation.NO_MATCH -> onClearFilter
                                HistoryPresentation.EMPTY -> onStartPractice
                                else -> onRetry
                            })
                            if (state.presentation == HistoryPresentation.LOAD_ERROR) SecondaryButton(stringResource(R.string.goal_assessment_return_to_path), onReturnToPath)
                        }
                    }
                }
                if (state.isLoadingMore || state.hasLoadMoreError) {
                    item(key = "history-more-status", contentType = HistoryUiState::class) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (state.isLoadingMore) SpeakMateLoadingIndicator()
                            if (state.hasLoadMoreError) {
                                Text(stringResource(R.string.history_load_more_error), style = DesignTypography.Detail, color = DesignColor.Error)
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
