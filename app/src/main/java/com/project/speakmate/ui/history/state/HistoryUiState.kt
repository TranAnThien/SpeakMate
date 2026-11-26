package com.project.speakmate.ui.history.state

import androidx.compose.runtime.Immutable
import com.project.speakmate.ui.components.ContentFilterUiState
import com.project.speakmate.utils.AppConstant

@Immutable
data class HistoryUiState(
    val presentation: HistoryPresentation = HistoryPresentation.POPULATED,
    val filters: List<ContentFilterUiState> = emptyList(),
    val selectedFilterId: String = AppConstant.HISTORY_FILTER_ALL,
    val groups: List<HistoryGroupUiState> = emptyList(),
    val pendingSessions: List<HistoryPendingSessionUiState> = emptyList(),
    val pendingDiscardId: String? = null,
    val refreshVersion: Int = 0,
    val isRefreshing: Boolean = false,
    val hasRefreshError: Boolean = false,
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasLoadMoreError: Boolean = false,
    val pendingNavigation: HistoryNavigation? = null
)
