package com.project.speakmate.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.model.PendingPracticeCompletion
import com.project.speakmate.domain.model.PracticeAttempt
import com.project.speakmate.domain.model.PracticeCompletionStatus
import com.project.speakmate.domain.model.PracticeType
import com.project.speakmate.domain.model.PracticeHistoryCursor
import com.project.speakmate.domain.repository.AuthRepository
import com.project.speakmate.domain.repository.HistoryRepository
import com.project.speakmate.domain.repository.PracticeCompletionRepository
import com.project.speakmate.domain.repository.PracticeRepository
import com.project.speakmate.domain.rules.PracticeHistoryRules
import com.project.speakmate.ui.history.state.HistoryNavigation
import com.project.speakmate.ui.history.state.HistoryPresentation
import com.project.speakmate.ui.history.state.HistoryUiState
import com.project.speakmate.utils.AppConstant
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@HiltViewModel
class HistoryViewModel
@Inject
constructor(
    private val historyRepository: HistoryRepository,
    private val practiceRepository: PracticeRepository,
    private val completionRepository: PracticeCompletionRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    companion object {
        private const val STATE_SUBSCRIPTION_TIMEOUT_MILLIS = 5_000L
        private const val TITLE_CONCURRENCY = 3
        private const val TITLE_TIMEOUT_MILLIS = 3_000L
    }

    private var paging = PagingState()

    private val savedState = MutableStateFlow(
        HistoryUiState(presentation = HistoryPresentation.LOADING, filters = HistoryUiMapper.defaultFilters())
    )
    private val pending = MutableStateFlow<List<PendingPracticeCompletion>>(emptyList())
    private val pendingDiscardId = MutableStateFlow<String?>(null)
    private var currentUserId: String? = null
    private var isStarted = false
    private var observerJob: Job? = null
    private var hasObserverError = false

    val uiState: StateFlow<HistoryUiState> =
        combine(savedState, pending, pendingDiscardId) { history, completions, discardId ->
            HistoryUiMapper.mergePending(history = history, completions = completions, discardId = discardId)
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STATE_SUBSCRIPTION_TIMEOUT_MILLIS),
                initialValue = savedState.value
            )

    init {
        observeUser()
    }

    private fun observeUser() {
        observerJob?.cancel()
        hasObserverError = false
        observerJob =
            viewModelScope.launch {
                try {
                    authRepository.observeSignedInUserId().distinctUntilChanged().collectLatest { uid ->
                        try {
                            hasObserverError = false
                            paging.historyGeneration++
                            paging.pageJob?.cancel()
                            paging.titlesJob?.cancel()
                            paging = PagingState(historyGeneration = paging.historyGeneration)
                            currentUserId = uid
                            pending.value = emptyList()
                            pendingDiscardId.value = null
                            savedState.value =
                                HistoryUiState(
                                    presentation = HistoryPresentation.LOADING,
                                    filters = HistoryUiMapper.defaultFilters(),
                                    selectedFilterId = AppConstant.HISTORY_FILTER_ALL
                                )
                            if (uid == null) return@collectLatest
                            if (isStarted) readPage(isRefresh = true)
                            completionRepository.observePendingCompletions().collect { latest ->
                                if (!isCurrentUser(uid)) return@collect
                                val removed =
                                    paging.previousPending.filter { previous ->
                                        latest.none { it.attemptId == previous.attemptId }
                                    }
                                val bridge = PracticeHistoryRules.getPendingBridge(removed, paging.attempts)
                                paging.previousPending = latest.toList()
                                bridge.forEach { paging.pendingBridge[it.attemptId] = it }
                                pending.value = (latest + paging.pendingBridge.values).distinctBy { it.attemptId }
                                if (removed.isNotEmpty()) {
                                    paging.shouldRefreshPending = true
                                    if (isStarted && !savedState.value.isRefreshing) retry()
                                }
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            showObserverError()
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    showObserverError()
                }
            }
    }

    private fun showObserverError() {
        hasObserverError = true
        paging.historyGeneration++
        paging.pageJob?.cancel()
        paging.titlesJob?.cancel()
        pending.value = emptyList()
        savedState.value = HistoryUiState(
            presentation = HistoryPresentation.LOAD_ERROR,
            filters = HistoryUiMapper.defaultFilters()
        )
    }

    fun onStarted() {
        if (isStarted) return
        isStarted = true
        retry()
    }

    fun onStopped() {
        isStarted = false
        paging.historyGeneration++
        paging.pageJob?.cancel()
        paging.titlesJob?.cancel()
        savedState.update { it.copy(isRefreshing = false, isLoadingMore = false) }
    }

    fun selectFilter(filterId: String) {
        if (HistoryUiMapper.defaultFilters().none { it.id == filterId } ||
            savedState.value.selectedFilterId == filterId)
            return
        paging.historyGeneration++
        paging.pageJob?.cancel()
        paging.titlesJob?.cancel()
        paging.attempts = emptyList()
        paging.nextCursor = null
        paging.hasLoadedHistory = false
        savedState.update {
            HistoryUiState(
                presentation = HistoryPresentation.LOADING,
                filters = HistoryUiMapper.defaultFilters(),
                selectedFilterId = filterId,
                refreshVersion = it.refreshVersion
            )
        }
        retry()
    }

    fun clearFilter() = selectFilter(AppConstant.HISTORY_FILTER_ALL)

    fun requestDiscard(attemptId: String) {
        if (uiState.value.pendingSessions.any { it.id == attemptId && HistoryUiMapper.canDiscard(it.status) })
            pendingDiscardId.value = attemptId
    }

    fun cancelDiscard() {
        pendingDiscardId.value = null
    }

    fun confirmDiscard() {
        val id = uiState.value.pendingDiscardId ?: return
        paging.previousPending = paging.previousPending.filterNot { it.attemptId == id }
        completionRepository.discardCompletion(id)
        pendingDiscardId.value = null
    }

    fun reload() {
        savedState.update { it.copy(refreshVersion = it.refreshVersion + 1) }
        retry()
    }

    fun startPractice() {
        val destination =
            if (savedState.value.selectedFilterId == AppConstant.HISTORY_FILTER_FREE_PRACTICE) {
                HistoryNavigation.OPEN_PRACTICE
            } else {
                HistoryNavigation.OPEN_TRAINING_PATH
            }
        savedState.update { it.copy(pendingNavigation = destination) }
    }

    fun consumePendingNavigation(): HistoryNavigation? {
        while (true) {
            val current = savedState.value
            val destination = current.pendingNavigation ?: return null
            if (savedState.compareAndSet(current, current.copy(pendingNavigation = null))) {
                return destination
            }
        }
    }

    fun retry() {
        if (hasObserverError || observerJob?.isActive != true) observeUser() else readPage(isRefresh = true)
    }

    fun loadMore() {
        readPage(isRefresh = false)
    }

    private fun readPage(isRefresh: Boolean) {
        val uid = currentUserId ?: return
        val before = savedState.value
        if (!isStarted || !isCurrentUser(uid)) return
        if (!isRefresh && (!before.hasMore || before.isLoadingMore || before.isRefreshing || before.hasRefreshError))
            return
        if (isRefresh && before.isRefreshing) return
        val cursor = if (isRefresh) null else paging.nextCursor ?: return
        val bridgeAtRequest = paging.pendingBridge.keys.toSet()
        val type = HistoryUiMapper.getPracticeType(before.selectedFilterId)
        paging.pageJob?.cancel()
        paging.titlesJob?.cancel()
        val generation = ++paging.historyGeneration
        if (isRefresh) paging.shouldRefreshPending = false
        savedState.update {
            HistoryUiMapper.toPageLoading(state = it, hasLoadedHistory = paging.hasLoadedHistory, isRefresh = isRefresh)
        }
        paging.pageJob =
            viewModelScope.launch {
                try {
                    readPageResult(
                        uid = uid,
                        type = type,
                        cursor = cursor,
                        generation = generation,
                        isRefresh = isRefresh,
                        bridgeAtRequest = bridgeAtRequest
                    )
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    showPageError(uid = uid, generation = generation, isRefresh = isRefresh)
                } finally {
                    if (generation == paging.historyGeneration && paging.shouldRefreshPending && isStarted) retry()
                }
            }
    }

    private fun showPageError(uid: String, generation: Int, isRefresh: Boolean) {
        if (generation == paging.historyGeneration && isCurrentUser(uid))
            savedState.update {
                HistoryUiMapper.toPageFailure(
                    state = it,
                    hasLoadedHistory = paging.hasLoadedHistory,
                    isRefresh = isRefresh
                )
            }
        paging.shouldRefreshPending = false
    }

    private suspend fun readPageResult(
        uid: String,
        type: PracticeType?,
        cursor: PracticeHistoryCursor?,
        generation: Int,
        isRefresh: Boolean,
        bridgeAtRequest: Set<String>
    ) {
        val page = historyRepository.getPracticeHistoryPage(type, cursor).getOrThrow()
        currentCoroutineContext().ensureActive()
        if (generation != paging.historyGeneration || !isCurrentUser(uid)) return
        val merged = PracticeHistoryRules.mergePage(
            attempts = paging.attempts,
            page = page,
            cursor = cursor,
            isRefresh = isRefresh
        )
        val groups = HistoryUiMapper.toGroups(merged.attempts, paging.scenarioTitles)
        if (generation != paging.historyGeneration || !isCurrentUser(uid)) return
        paging.attempts = merged.attempts
        paging.nextCursor = merged.nextCursor
        paging.hasLoadedHistory = true
        savedState.update {
            HistoryUiMapper.toPageLoaded(
                state = it,
                page = merged,
                groups = groups,
                isPageEmpty = page.attempts.isEmpty()
            )
        }
        if (isRefresh) {
            bridgeAtRequest.forEach(paging.pendingBridge::remove)
            pending.value =
                (paging.previousPending + paging.pendingBridge.values).distinctBy { it.attemptId }
        }
        resolveTitles(uid = uid, generation = generation, shouldRefresh = isRefresh)
    }

    private fun resolveTitles(uid: String, generation: Int, shouldRefresh: Boolean) {
        paging.titlesJob =
            viewModelScope.launch {
                paging.attempts
                    .map { it.scenarioId }
                    .filter(String::isNotBlank)
                    .distinct()
                    .filter { shouldRefresh || it !in paging.scenarioTitles }
                    .chunked(TITLE_CONCURRENCY)
                    .forEach { batch ->
                        val titles = coroutineScope {
                            batch
                                .map { id ->
                                    async {
                                        val title =
                                            try {
                                                withTimeoutOrNull(TITLE_TIMEOUT_MILLIS) {
                                                    practiceRepository
                                                        .getScenario(id)
                                                        .getOrThrow()
                                                        .name
                                                        .takeIf(String::isNotBlank)
                                                }
                                            } catch (cancelled: CancellationException) {
                                                throw cancelled
                                            } catch (_: Exception) {
                                                null
                                            }
                                        id to title
                                    }
                                }
                                .awaitAll()
                        }
                        if (generation != paging.historyGeneration || !isCurrentUser(uid)) return@launch
                        titles.forEach { (id, title) -> if (title != null) paging.scenarioTitles[id] = title }
                        val groups = HistoryUiMapper.toGroups(paging.attempts, paging.scenarioTitles)
                        if (generation != paging.historyGeneration || !isCurrentUser(uid)) return@launch
                        savedState.update { it.copy(groups = groups) }
                    }
            }
    }

    fun retrySave(attemptId: String) {
        val item = uiState.value.pendingSessions.firstOrNull { it.id == attemptId } ?: return
        if (item.status == PracticeCompletionStatus.AWAITING_GRADING) completionRepository.retryGrading(attemptId)
        else if (item.status == PracticeCompletionStatus.SAVE_FAILED && item.canRetrySave)
            completionRepository.retrySave(attemptId)
    }

    fun saveUnassessed(attemptId: String) {
        if (uiState.value.pendingSessions.any {
            it.id == attemptId && it.status == PracticeCompletionStatus.AWAITING_GRADING
        })
            completionRepository.saveUnassessed(attemptId)
    }

    private fun isCurrentUser(uid: String): Boolean =
        try {
            currentUserId == uid && authRepository.requireCurrentUserId() == uid
        } catch (_: IllegalStateException) {
            false
        }

    private class PagingState(var historyGeneration: Int = 0) {
        val scenarioTitles = mutableMapOf<String, String>()
        var previousPending = emptyList<PendingPracticeCompletion>()
        val pendingBridge = linkedMapOf<String, PendingPracticeCompletion>()
        var attempts = emptyList<PracticeAttempt>()
        var nextCursor: PracticeHistoryCursor? = null
        var pageJob: Job? = null
        var titlesJob: Job? = null
        var hasLoadedHistory = false
        var shouldRefreshPending = false
    }
}
