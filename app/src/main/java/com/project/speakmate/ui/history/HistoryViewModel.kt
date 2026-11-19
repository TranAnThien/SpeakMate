package com.project.speakmate.ui.history

import com.project.speakmate.utils.AppConstant
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.PracticeAttempt
import com.project.speakmate.domain.model.PracticeType
import com.project.speakmate.domain.model.durationInSeconds
import com.project.speakmate.domain.repository.HistoryRepository
import com.project.speakmate.domain.repository.PracticeRepository
import com.project.speakmate.ui.components.ContentFilterUiState
import com.project.speakmate.ui.history.state.HistoryGroupUiState
import com.project.speakmate.ui.history.state.HistoryPresentation
import com.project.speakmate.ui.history.state.HistoryResultPresentation
import com.project.speakmate.ui.history.state.HistorySessionUiState
import com.project.speakmate.ui.history.state.HistoryUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val practiceRepository: PracticeRepository
) : ViewModel() {

    private val selectedFilter = MutableStateFlow(AppConstant.HISTORY_FILTER_ALL)
    private val loadRequest = MutableStateFlow(0)
    private val dateFormatter = SimpleDateFormat(AppConstant.DISPLAY_DATE_PATTERN, Locale.getDefault())
    private val timeFormatter = SimpleDateFormat(AppConstant.DISPLAY_TIME_PATTERN, Locale.getDefault())
    private val scenarioTitleCache = ConcurrentHashMap<String, String>()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HistoryUiState> = loadRequest.flatMapLatest {
        observeHistory()
            .onStart { emit(loadState(HistoryPresentation.LOADING)) }
            .catch { failure ->
                if (failure is CancellationException) throw failure
                emit(loadState(HistoryPresentation.LOAD_ERROR))
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(AppConstant.STATE_SUBSCRIPTION_TIMEOUT_MILLIS),
        initialValue = loadState(HistoryPresentation.LOADING)
    )

    fun onSelectFilter(filterId: String) {
        selectedFilter.value = filterId
    }

    fun retry() {
        loadRequest.update { it + 1 }
    }

    private fun loadState(presentation: HistoryPresentation) = HistoryUiState(
        presentation = presentation,
        filters = defaultFilters(),
        selectedFilterId = selectedFilter.value
    )

    private fun observeHistory(): Flow<HistoryUiState> = combine(
        flow { emitAll(historyRepository.observePracticeHistory()) },
        selectedFilter
    ) { attempts, filterId ->
        val filtered = when (filterId) {
            AppConstant.HISTORY_FILTER_FREE_PRACTICE -> attempts.filter { it.practiceType == PracticeType.FREE_PRACTICE }
            AppConstant.HISTORY_FILTER_LESSONS -> attempts.filter {
                it.practiceType == PracticeType.TRAINING_UNIT || it.practiceType == PracticeType.GOAL_ASSESSMENT
            }
            else -> attempts
        }

        if (filtered.isEmpty()) {
            HistoryUiState(
                presentation = HistoryPresentation.EMPTY,
                filters = defaultFilters(),
                selectedFilterId = filterId,
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
                            val title = practiceRepository.getScenario(id).getOrNull()?.name?.takeIf { it.isNotBlank() } ?: id
                            scenarioTitleCache[id] = title
                            id to title
                        }
                    }
                }.awaitAll().toMap()
            }

            val groups = filtered.groupBy { dateFormatter.format(it.startedAt) }
                .map { (date, items) ->
                    HistoryGroupUiState(
                        id = date,
                        title = date,
                        sessions = items.map { attempt ->
                            val resolvedTitle = titles[attempt.scenarioId]?.takeIf { it.isNotBlank() }
                                ?: attempt.scenarioId
                            toSessionUiState(attempt, resolvedTitle)
                        }
                    )
                }
            HistoryUiState(
                presentation = HistoryPresentation.POPULATED,
                filters = defaultFilters(),
                selectedFilterId = filterId,
                groups = groups
            )
        }
    }

    private fun toSessionUiState(attempt: PracticeAttempt, scenarioTitle: String): HistorySessionUiState {
        val duration = String.format(Locale.ROOT, AppConstant.DURATION_FORMAT,
            attempt.durationInSeconds / AppConstant.SECONDS_PER_MINUTE, attempt.durationInSeconds % AppConstant.SECONDS_PER_MINUTE)
        return HistorySessionUiState(
            id = attempt.attemptId,
            title = scenarioTitle,
            result = HistoryResultMapper.toPresentation(attempt.result, attempt.practiceType),
            practiceType = attempt.practiceType,
            duration = duration,
            performedAt = timeFormatter.format(attempt.startedAt)
        )
    }

    private fun defaultFilters() = listOf(
        ContentFilterUiState(AppConstant.HISTORY_FILTER_ALL, ""),
        ContentFilterUiState(AppConstant.HISTORY_FILTER_LESSONS, ""),
        ContentFilterUiState(AppConstant.HISTORY_FILTER_FREE_PRACTICE, "")
    )
}
