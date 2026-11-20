package com.project.speakmate.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.model.PracticeAttempt
import com.project.speakmate.domain.model.PracticeType
import com.project.speakmate.domain.model.PracticeCompletionStatus
import com.project.speakmate.domain.model.durationInSeconds
import com.project.speakmate.domain.repository.HistoryRepository
import com.project.speakmate.domain.repository.PracticeRepository
import com.project.speakmate.domain.repository.PracticeCompletionRepository
import com.project.speakmate.ui.components.ContentFilterUiState
import com.project.speakmate.ui.history.state.HistoryGroupUiState
import com.project.speakmate.ui.history.state.HistoryPresentation
import com.project.speakmate.ui.history.state.HistorySessionUiState
import com.project.speakmate.ui.history.state.HistoryUiState
import com.project.speakmate.ui.history.state.HistoryPendingSessionUiState
import com.project.speakmate.utils.AppConstant
import com.project.speakmate.utils.extension.default
import com.project.speakmate.utils.extension.toDateLabel
import com.project.speakmate.utils.extension.toDurationLabel
import com.project.speakmate.utils.extension.toTimeLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.ConcurrentHashMap
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val practiceRepository: PracticeRepository,
    private val completionRepository: PracticeCompletionRepository
) : ViewModel() {

    private val selectedFilter = MutableStateFlow(AppConstant.HISTORY_FILTER_ALL)
    private val loadRequest = MutableStateFlow(0)
    private val scenarioTitleCache = ConcurrentHashMap<String, String>()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HistoryUiState> = combine(loadRequest.flatMapLatest {
        observeHistory()
            .onStart { emit(loadState(HistoryPresentation.LOADING)) }
            .catch { failure ->
                if (failure is CancellationException) throw failure
                emit(loadState(HistoryPresentation.LOAD_ERROR))
            }
    }, completionRepository.observePendingCompletions(), selectedFilter) { history, pending, filterId ->
        val visiblePending = pending.distinctBy { it.attemptId }.filter { matchesFilter(it.practiceType, filterId) }
        val pendingIds = visiblePending.map { it.attemptId }.toSet()
        val groups = history.groups.mapNotNull { group ->
            val sessions = group.sessions.filter { it.id !in pendingIds && matchesFilter(it.practiceType, filterId) }
            group.copy(sessions = sessions).takeIf { sessions.isNotEmpty() }
        }
        history.copy(
            presentation = if (history.presentation == HistoryPresentation.POPULATED && groups.isEmpty()) HistoryPresentation.EMPTY else history.presentation,
            selectedFilterId = filterId,
            groups = groups,
            pendingSessions = visiblePending.map {
                val startedAt = Date(it.startedAtEpochMillis)
                HistoryPendingSessionUiState(
                    id = it.attemptId,
                    title = it.scenarioTitle,
                    practiceType = it.practiceType,
                    performedAt = startedAt.toTimeLabel(),
                    performedDate = startedAt.toDateLabel(),
                    status = it.status
                )
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(AppConstant.STATE_SUBSCRIPTION_TIMEOUT_MILLIS),
        initialValue = loadState(HistoryPresentation.LOADING)
    )

    fun selectFilter(filterId: String) {
        selectedFilter.value = filterId
    }

    fun retry() {
        loadRequest.update { it + 1 }
    }

    fun retrySave(attemptId: String) {
        if (uiState.value.pendingSessions.none { it.id == attemptId && it.status == PracticeCompletionStatus.SAVE_FAILED }) return
        completionRepository.retrySave(attemptId)
    }

    private fun matchesFilter(type: PracticeType?, filterId: String): Boolean = when (filterId) {
        AppConstant.HISTORY_FILTER_FREE_PRACTICE -> type == PracticeType.FREE_PRACTICE
        AppConstant.HISTORY_FILTER_LESSONS -> type == PracticeType.TRAINING_UNIT || type == PracticeType.GOAL_ASSESSMENT
        else -> true
    }

    private fun loadState(presentation: HistoryPresentation) = HistoryUiState(
        presentation = presentation,
        filters = defaultFilters(),
        selectedFilterId = selectedFilter.value
    )

    private fun observeHistory(): Flow<HistoryUiState> = flow {
        emitAll(historyRepository.observePracticeHistory())
    }.map { attempts ->
        val filtered = attempts.distinctBy { it.attemptId }

        if (filtered.isEmpty()) {
            HistoryUiState(
                presentation = HistoryPresentation.EMPTY,
                filters = defaultFilters(),
                groups = emptyList()
            )
        } else {
            val distinctScenarioIds = filtered.map { it.scenarioId }.filter { it.isNotBlank() }.distinct()
            val titles = coroutineScope {
                distinctScenarioIds.map { id ->
                    async {
                        val cached = scenarioTitleCache[id]
                        if (cached != null) {
                            id to cached
                        } else {
                            val title = practiceRepository.getScenario(id).getOrNull()?.name?.takeIf { it.isNotBlank() }.default(id)
                            scenarioTitleCache[id] = title
                            id to title
                        }
                    }
                }.awaitAll().toMap()
            }

            val groups = filtered.groupBy { it.startedAt.toDateLabel() }
                .map { (date, items) ->
                    HistoryGroupUiState(
                        id = date,
                        title = date,
                        sessions = items.map { attempt ->
                            val resolvedTitle = titles[attempt.scenarioId]?.takeIf { it.isNotBlank() }.default(attempt.scenarioId)
                            toSessionUiState(attempt, resolvedTitle)
                        }
                    )
                }
            HistoryUiState(
                presentation = HistoryPresentation.POPULATED,
                filters = defaultFilters(),
                groups = groups
            )
        }
    }

    private fun toSessionUiState(attempt: PracticeAttempt, scenarioTitle: String): HistorySessionUiState {
        val duration = attempt.durationInSeconds.toDurationLabel()
        return HistorySessionUiState(
            id = attempt.attemptId,
            title = scenarioTitle,
            result = HistoryResultMapper.toPresentation(attempt.result, attempt.practiceType),
            practiceType = attempt.practiceType,
            duration = duration,
            performedAt = attempt.startedAt.toTimeLabel()
        )
    }

    private fun defaultFilters() = listOf(
        ContentFilterUiState(AppConstant.HISTORY_FILTER_ALL, ""),
        ContentFilterUiState(AppConstant.HISTORY_FILTER_LESSONS, ""),
        ContentFilterUiState(AppConstant.HISTORY_FILTER_FREE_PRACTICE, "")
    )
}
