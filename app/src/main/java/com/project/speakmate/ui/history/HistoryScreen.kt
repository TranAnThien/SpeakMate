package com.project.speakmate.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.AppBottomBar
import com.project.speakmate.ui.components.AppDestination
import com.project.speakmate.ui.components.ContentErrorDialog
import com.project.speakmate.ui.components.ContentFilterRow
import com.project.speakmate.ui.components.ContentFilterUiState
import com.project.speakmate.ui.components.ContentPageHeader
import com.project.speakmate.ui.components.ContentSkeletonCards
import com.project.speakmate.ui.components.ContentStatusPanel
import com.project.speakmate.ui.history.components.HistorySessionCard
import com.project.speakmate.ui.history.state.HistoryGroupUiState
import com.project.speakmate.ui.history.state.HistoryPresentation
import com.project.speakmate.ui.history.state.HistorySessionUiState
import com.project.speakmate.ui.history.state.HistoryUiState
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onSelectFilter: (String) -> Unit,
    onOpenSession: (String) -> Unit,
    onStartPractice: () -> Unit,
    onRetry: () -> Unit,
    onDismissError: () -> Unit,
    onOpenTrainingPath: () -> Unit,
    onOpenPractice: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize().background(DesignColor.Background)) {
        Column(Modifier.fillMaxSize()) {
            ContentPageHeader(stringResource(R.string.navigation_history))
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item(key = "history-filters", contentType = ContentFilterUiState::class) {
                    ContentFilterRow(state.filters, state.selectedFilterId, onSelectFilter, Modifier.padding(bottom = 8.dp))
                }
                when (state.presentation) {
                    HistoryPresentation.POPULATED -> state.groups.forEachIndexed { groupIndex, group ->
                        item(key = "group:$groupIndex:${group.id}", contentType = HistoryGroupUiState::class) {
                            Text(group.title, style = DesignTypography.Label, color = DesignColor.Muted, modifier = Modifier.padding(bottom = 8.dp))
                        }
                        itemsIndexed(group.sessions,
                            key = { index, session -> "session:$groupIndex:$index:${session.id}" },
                            contentType = { _, _ -> HistorySessionUiState::class }) { index, session ->
                            val onOpen = remember(session.id, onOpenSession) { { onOpenSession(session.id) } }
                            HistorySessionCard(session, onOpen, Modifier.padding(bottom = if (index == group.sessions.lastIndex) 8.dp else 0.dp))
                        }
                    }
                    HistoryPresentation.LOADING -> item(key = "history-loading", contentType = HistoryPresentation.LOADING) {
                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            Text(stringResource(R.string.history_loading), style = DesignTypography.Detail, color = DesignColor.Primary)
                            ContentSkeletonCards(4)
                        }
                    }
                    HistoryPresentation.EMPTY, HistoryPresentation.LOAD_ERROR -> item(key = "history-status", contentType = state.presentation) {
                        ContentStatusPanel(
                            iconRes = if (state.presentation == HistoryPresentation.EMPTY) R.drawable.img_placement_mascot else R.drawable.ic_load_error,
                            title = stringResource(if (state.presentation == HistoryPresentation.EMPTY) R.string.history_empty_title else R.string.history_error_title),
                            description = stringResource(if (state.presentation == HistoryPresentation.EMPTY) R.string.history_empty_description else R.string.history_error_description),
                            primaryText = stringResource(if (state.presentation == HistoryPresentation.EMPTY) R.string.history_start_practice else R.string.content_reload),
                            secondaryText = if (state.presentation == HistoryPresentation.EMPTY) "" else stringResource(R.string.goal_assessment_return_to_path),
                            onPrimaryAction = if (state.presentation == HistoryPresentation.EMPTY) onStartPractice else onRetry,
                            onSecondaryAction = onOpenTrainingPath,
                            isMascot = state.presentation == HistoryPresentation.EMPTY
                        )
                    }
                    HistoryPresentation.ERROR_DIALOG -> Unit
                }
            }
            AppBottomBar(onOpenTrainingPath, onOpenPractice, onOpenHistory, onOpenProfile,
                selectedDestination = AppDestination.HISTORY, bottomPadding = 4.dp)
        }
        if (state.presentation == HistoryPresentation.ERROR_DIALOG) {
            ContentErrorDialog(stringResource(R.string.history_error_title), stringResource(R.string.history_error_dialog_description), onRetry, onDismissError)
        }
    }
}
