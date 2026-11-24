package com.project.speakmate.ui.history

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.project.speakmate.ui.history.state.HistoryNavigation
import com.project.speakmate.utils.AppConstant
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
fun HistoryRoute(
    onOpenSession: (String) -> Unit,
    onOpenPractice: () -> Unit,
    onOpenTrainingPath: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val owner = LocalLifecycleOwner.current
    val openPractice by rememberUpdatedState(onOpenPractice)
    val openTrainingPath by rememberUpdatedState(onOpenTrainingPath)
    LaunchedEffect(state.pendingNavigation, viewModel, owner) {
        if (state.pendingNavigation != null) {
            owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                when (viewModel.consumePendingNavigation()) {
                    HistoryNavigation.OPEN_PRACTICE -> openPractice()
                    HistoryNavigation.OPEN_TRAINING_PATH -> openTrainingPath()
                    null -> Unit
                }
            }
        }
    }
    LaunchedEffect(viewModel, owner, listState) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.onStarted()
            try {
                combine(
                    snapshotFlow {
                        val layout = listState.layoutInfo
                        val last = layout.visibleItemsInfo.lastOrNull()?.index
                        last to layout.totalItemsCount
                    },
                    viewModel.uiState
                ) { (last, total), current ->
                    current.hasMore &&
                        !current.isLoadingMore &&
                        !current.hasLoadMoreError &&
                        !current.isRefreshing &&
                        !current.hasRefreshError &&
                        last != null &&
                        last >= total - AppConstant.HISTORY_PREFETCH_ITEM_COUNT
                }
                    .distinctUntilChanged()
                    .filter { it }
                    .collect { viewModel.loadMore() }
            } finally {
                viewModel.onStopped()
            }
        }
    }
    LaunchedEffect(state.refreshVersion, state.selectedFilterId) { listState.scrollToItem(0) }
    HistoryScreen(
        state = state,
        onSelectFilter = viewModel::selectFilter,
        onOpenSession = onOpenSession,
        onStartPractice = viewModel::startPractice,
        onRetry = viewModel::reload,
        onRetrySave = viewModel::retrySave,
        onSaveUnassessed = viewModel::saveUnassessed,
        onRequestDiscard = viewModel::requestDiscard,
        onConfirmDiscard = viewModel::confirmDiscard,
        onCancelDiscard = viewModel::cancelDiscard,
        onClearFilter = viewModel::clearFilter,
        onReturnToPath = onOpenTrainingPath,
        onLoadMore = viewModel::loadMore,
        listState = listState,
        modifier = modifier
    )
}
