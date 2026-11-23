package com.project.speakmate.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.model.PendingPracticeCompletion
import com.project.speakmate.domain.model.PracticeAttempt
import com.project.speakmate.domain.model.PracticeCompletionStatus
import com.project.speakmate.domain.model.PracticeHistoryCursor
import com.project.speakmate.domain.model.PracticeType
import com.project.speakmate.domain.model.durationInSeconds
import com.project.speakmate.domain.repository.AuthRepository
import com.project.speakmate.domain.repository.HistoryRepository
import com.project.speakmate.domain.repository.PracticeCompletionRepository
import com.project.speakmate.domain.repository.PracticeRepository
import com.project.speakmate.domain.rules.PracticeHistoryRules
import com.project.speakmate.ui.components.ContentFilterUiState
import com.project.speakmate.ui.history.state.HistoryGroupUiState
import com.project.speakmate.ui.history.state.HistoryPendingSessionUiState
import com.project.speakmate.ui.history.state.HistoryPresentation
import com.project.speakmate.ui.history.state.HistorySessionUiState
import com.project.speakmate.ui.history.state.HistoryUiState
import com.project.speakmate.utils.AppConstant
import com.project.speakmate.utils.extension.defaultEmpty
import com.project.speakmate.utils.extension.toDurationLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val practiceRepository: PracticeRepository,
    private val completionRepository: PracticeCompletionRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    companion object {
        private const val TITLE_CONCURRENCY = 3
        private const val TITLE_TIMEOUT_MILLIS = 3_000L
    }

    private val savedState = MutableStateFlow(HistoryUiState(presentation = HistoryPresentation.LOADING, filters = defaultFilters()))
    private val pending = MutableStateFlow<List<PendingPracticeCompletion>>(emptyList())
    private val pendingDiscardId = MutableStateFlow<String?>(null)
    private val scenarioTitles = mutableMapOf<String, String>()
    private var previousPending = emptyList<PendingPracticeCompletion>()
    private val pendingBridge = linkedMapOf<String, PendingPracticeCompletion>()
    private var attempts = emptyList<PracticeAttempt>()
    private var nextCursor: PracticeHistoryCursor? = null
    private var pageJob: Job? = null
    private var titlesJob: Job? = null
    private var currentUserId: String? = null
    private var historyGeneration = 0
    private var hasLoadedHistory = false
    private var isStarted = false
    private var shouldRefreshPending = false
    private var observerJob: Job? = null
    private var hasObserverError = false

    val uiState: StateFlow<HistoryUiState> = combine(savedState, pending, pendingDiscardId) { history, completions, discardId ->
        val type = getPracticeType(history.selectedFilterId)
        val visible = completions.distinctBy { it.attemptId }.filter { type == null || it.practiceType == type }
        val pendingIds = visible.map { it.attemptId }.toSet()
        val groups = history.groups.mapNotNull { group ->
            group.copy(sessions = group.sessions.filter { it.id !in pendingIds }).takeIf { it.sessions.isNotEmpty() }
        }
        history.copy(
            presentation = if (history.presentation == HistoryPresentation.EMPTY && visible.isEmpty() && type != null) HistoryPresentation.NO_MATCH else history.presentation,
            groups = groups,
            pendingDiscardId = discardId.takeIf { id -> visible.any { it.attemptId == id && canDiscard(it.status) } },
            pendingSessions = visible.map {
                HistoryPendingSessionUiState(it.attemptId, it.scenarioTitle, it.practiceType,
                    it.startedAtEpochMillis, it.status, it.saveFailure)
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstant.STATE_SUBSCRIPTION_TIMEOUT_MILLIS), savedState.value)

    init { observeUser() }

    private fun observeUser() {
        observerJob?.cancel()
        hasObserverError = false
        observerJob = viewModelScope.launch {
            try {
                authRepository.observeSignedInUserId().distinctUntilChanged().collectLatest { uid ->
                    try {
                        hasObserverError = false
                        historyGeneration++
                        pageJob?.cancel()
                        titlesJob?.cancel()
                        currentUserId = uid
                        attempts = emptyList()
                        nextCursor = null
                        previousPending = emptyList()
                        pendingBridge.clear()
                        pending.value = emptyList()
                        pendingDiscardId.value = null
                        scenarioTitles.clear()
                        hasLoadedHistory = false
                        shouldRefreshPending = false
                        savedState.value = HistoryUiState(presentation = HistoryPresentation.LOADING, filters = defaultFilters(), selectedFilterId = AppConstant.HISTORY_FILTER_ALL)
                        if (uid == null) return@collectLatest
                        if (isStarted) readPage(isRefresh = true)
                        completionRepository.observePendingCompletions().collect { latest ->
                            if (!isCurrentUser(uid)) return@collect
                            val removed = previousPending.filter { previous -> latest.none { it.attemptId == previous.attemptId } }
                            val bridge = removed.filter { previous -> attempts.none { it.attemptId == previous.attemptId } }
                                .map { it.copy(status = PracticeCompletionStatus.SAVING) }
                            previousPending = latest.toList()
                            bridge.forEach { pendingBridge[it.attemptId] = it }
                            pending.value = (latest + pendingBridge.values).distinctBy { it.attemptId }
                            if (removed.isNotEmpty()) {
                                shouldRefreshPending = true
                                if (isStarted && !savedState.value.isRefreshing) retry()
                            }
                        }
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { showObserverError() }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                showObserverError()
            }
        }
    }

    private fun showObserverError() {
        hasObserverError = true
        historyGeneration++
        pageJob?.cancel()
        titlesJob?.cancel()
        pending.value = emptyList()
        savedState.value = HistoryUiState(presentation = HistoryPresentation.LOAD_ERROR, filters = defaultFilters())
    }

    fun onStarted() {
        if (isStarted) return
        isStarted = true
        retry()
    }

    fun onStopped() {
        isStarted = false
        historyGeneration++
        pageJob?.cancel()
        titlesJob?.cancel()
        savedState.update { it.copy(isRefreshing = false, isLoadingMore = false) }
    }

    fun selectFilter(filterId: String) {
        if (defaultFilters().none { it.id == filterId } || savedState.value.selectedFilterId == filterId) return
        historyGeneration++
        pageJob?.cancel()
        titlesJob?.cancel()
        attempts = emptyList()
        nextCursor = null
        hasLoadedHistory = false
        savedState.update { HistoryUiState(presentation = HistoryPresentation.LOADING, filters = defaultFilters(), selectedFilterId = filterId,
            refreshVersion = it.refreshVersion) }
        retry()
    }

    fun clearFilter() = selectFilter(AppConstant.HISTORY_FILTER_ALL)

    fun requestDiscard(attemptId: String) {
        if (uiState.value.pendingSessions.any { it.id == attemptId && canDiscard(it.status) }) pendingDiscardId.value = attemptId
    }

    fun cancelDiscard() { pendingDiscardId.value = null }

    private fun canDiscard(status: PracticeCompletionStatus): Boolean =
        status == PracticeCompletionStatus.AWAITING_GRADING || status == PracticeCompletionStatus.SAVE_FAILED

    fun confirmDiscard() {
        val id = uiState.value.pendingDiscardId ?: return
        previousPending = previousPending.filterNot { it.attemptId == id }
        completionRepository.discardCompletion(id)
        pendingDiscardId.value = null
    }

    fun reload() {
        savedState.update { it.copy(refreshVersion = it.refreshVersion + 1) }
        retry()
    }

    fun startPractice(): Boolean = savedState.value.selectedFilterId == AppConstant.HISTORY_FILTER_FREE_PRACTICE

    fun retry() {
        if (hasObserverError || observerJob?.isActive != true) observeUser()
        else readPage(isRefresh = true)
    }

    fun loadMore() { readPage(isRefresh = false) }

    private fun readPage(isRefresh: Boolean) {
        val uid = currentUserId ?: return
        val before = savedState.value
        if (!isStarted || !isCurrentUser(uid)) return
        if (!isRefresh && (!before.hasMore || before.isLoadingMore || before.isRefreshing || before.hasRefreshError)) return
        if (isRefresh && before.isRefreshing) return
        val cursor = if (isRefresh) null else nextCursor ?: return
        val bridgeAtRequest = pendingBridge.keys.toSet()
        val type = getPracticeType(before.selectedFilterId)
        pageJob?.cancel()
        titlesJob?.cancel()
        val generation = ++historyGeneration
        if (isRefresh) shouldRefreshPending = false
        savedState.update { it.copy(presentation = if (hasLoadedHistory) it.presentation else HistoryPresentation.LOADING,
            isRefreshing = isRefresh, isLoadingMore = !isRefresh, hasRefreshError = false, hasLoadMoreError = false) }
        pageJob = viewModelScope.launch {
            try {
                val page = historyRepository.getPracticeHistoryPage(type, cursor).getOrThrow()
                currentCoroutineContext().ensureActive()
                if (generation != historyGeneration || !isCurrentUser(uid)) return@launch
                check(!page.hasMore || page.nextCursor != null && page.nextCursor != cursor)
                val nextAttempts = if (isRefresh) page.attempts.distinctBy { it.attemptId } else PracticeHistoryRules.mergeAttempts(attempts, page.attempts)
                val groups = toGroups(nextAttempts)
                if (generation != historyGeneration || !isCurrentUser(uid)) return@launch
                attempts = nextAttempts
                nextCursor = page.nextCursor
                hasLoadedHistory = true
                savedState.update { it.copy(presentation = if (attempts.isEmpty() && !page.hasMore) HistoryPresentation.EMPTY else HistoryPresentation.POPULATED,
                    groups = groups, hasMore = page.hasMore, isRefreshing = false, isLoadingMore = false,
                    hasLoadMoreError = page.attempts.isEmpty() && page.hasMore,
                    refreshVersion = it.refreshVersion) }
                if (isRefresh) {
                    bridgeAtRequest.forEach(pendingBridge::remove)
                    pending.value = (previousPending + pendingBridge.values).distinctBy { it.attemptId }
                }
                resolveTitles(uid, generation, isRefresh)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (generation == historyGeneration && isCurrentUser(uid)) savedState.update {
                    it.copy(presentation = if (hasLoadedHistory) it.presentation else HistoryPresentation.LOAD_ERROR,
                        isRefreshing = false, isLoadingMore = false, hasRefreshError = isRefresh && hasLoadedHistory,
                        hasLoadMoreError = !isRefresh)
                }
                shouldRefreshPending = false
            } finally {
                if (generation == historyGeneration && shouldRefreshPending && isStarted) retry()
            }
        }
    }

    private fun resolveTitles(uid: String, generation: Int, shouldRefresh: Boolean) {
        titlesJob = viewModelScope.launch {
            attempts.map { it.scenarioId }.filter(String::isNotBlank).distinct().filter { shouldRefresh || it !in scenarioTitles }
                .chunked(TITLE_CONCURRENCY).forEach { batch ->
                    val titles = coroutineScope { batch.map { id -> async {
                        val title = try {
                            withTimeoutOrNull(TITLE_TIMEOUT_MILLIS) { practiceRepository.getScenario(id).getOrThrow().name.takeIf(String::isNotBlank) }
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { null }
                        id to title
                    } }.awaitAll() }
                    if (generation != historyGeneration || !isCurrentUser(uid)) return@launch
                    titles.forEach { (id, title) -> if (title != null) scenarioTitles[id] = title }
                    val groups = toGroups(attempts)
                    if (generation != historyGeneration || !isCurrentUser(uid)) return@launch
                    savedState.update { it.copy(groups = groups) }
                }
        }
    }

    fun retrySave(attemptId: String) {
        val item = uiState.value.pendingSessions.firstOrNull { it.id == attemptId } ?: return
        if (item.status == PracticeCompletionStatus.AWAITING_GRADING) completionRepository.retryGrading(attemptId)
        else if (item.status == PracticeCompletionStatus.SAVE_FAILED && item.canRetrySave) completionRepository.retrySave(attemptId)
    }

    fun saveUnassessed(attemptId: String) {
        if (uiState.value.pendingSessions.any { it.id == attemptId && it.status == PracticeCompletionStatus.AWAITING_GRADING }) completionRepository.saveUnassessed(attemptId)
    }

    private fun isCurrentUser(uid: String): Boolean = try {
        currentUserId == uid && authRepository.requireCurrentUserId() == uid
    } catch (_: IllegalStateException) { false }

    private suspend fun toGroups(snapshot: List<PracticeAttempt>): List<HistoryGroupUiState> {
        val titles = scenarioTitles.toMap()
        return withContext(Dispatchers.Default) {
            snapshot.groupBy { it.startedAt.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() }.map { (date, entries) ->
                HistoryGroupUiState(date.toString(), entries.first().startedAt.time, entries.map { attempt ->
                    HistorySessionUiState(id = attempt.attemptId, title = titles[attempt.scenarioId].defaultEmpty(),
                        result = HistoryResultMapper.toPresentation(attempt.result, attempt.practiceType), practiceType = attempt.practiceType,
                        duration = attempt.durationInSeconds.toDurationLabel(), startedAtMillis = attempt.startedAt.time)
                })
            }
        }
    }

    private fun getPracticeType(filterId: String): PracticeType? = when (filterId) {
        AppConstant.HISTORY_FILTER_LESSONS -> PracticeType.TRAINING_UNIT
        AppConstant.HISTORY_FILTER_GOALS -> PracticeType.GOAL_ASSESSMENT
        AppConstant.HISTORY_FILTER_FREE_PRACTICE -> PracticeType.FREE_PRACTICE
        else -> null
    }

    private fun defaultFilters() = listOf(
        ContentFilterUiState(AppConstant.HISTORY_FILTER_ALL, ""),
        ContentFilterUiState(AppConstant.HISTORY_FILTER_LESSONS, ""),
        ContentFilterUiState(AppConstant.HISTORY_FILTER_GOALS, ""),
        ContentFilterUiState(AppConstant.HISTORY_FILTER_FREE_PRACTICE, "")
    )
}
