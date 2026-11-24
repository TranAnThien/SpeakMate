package com.project.speakmate.ui.history

import com.project.speakmate.domain.model.PendingPracticeCompletion
import com.project.speakmate.domain.model.PracticeHistoryPage
import com.project.speakmate.domain.model.PracticeAttempt
import com.project.speakmate.domain.model.PracticeCompletionStatus
import com.project.speakmate.domain.model.PracticeType
import com.project.speakmate.domain.model.durationInSeconds
import com.project.speakmate.ui.components.ContentFilterUiState
import com.project.speakmate.ui.history.state.HistoryGroupUiState
import com.project.speakmate.ui.history.state.HistoryPendingSessionUiState
import com.project.speakmate.ui.history.state.HistoryPresentation
import com.project.speakmate.ui.history.state.HistorySessionUiState
import com.project.speakmate.ui.history.state.HistoryUiState
import com.project.speakmate.utils.AppConstant
import com.project.speakmate.utils.extension.defaultEmpty
import com.project.speakmate.utils.extension.toDurationLabel
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object HistoryUiMapper {
    fun toPageLoading(state: HistoryUiState, hasLoadedHistory: Boolean, isRefresh: Boolean): HistoryUiState =
        state.copy(
            presentation = if (hasLoadedHistory) state.presentation else HistoryPresentation.LOADING,
            isRefreshing = isRefresh,
            isLoadingMore = !isRefresh,
            hasRefreshError = false,
            hasLoadMoreError = false
        )

    fun toPageLoaded(
        state: HistoryUiState,
        page: PracticeHistoryPage,
        groups: List<HistoryGroupUiState>,
        isPageEmpty: Boolean
    ): HistoryUiState =
        state.copy(
            presentation =
                if (page.attempts.isEmpty() && !page.hasMore) HistoryPresentation.EMPTY
                else HistoryPresentation.POPULATED,
            groups = groups,
            hasMore = page.hasMore,
            isRefreshing = false,
            isLoadingMore = false,
            hasLoadMoreError = isPageEmpty && page.hasMore
        )

    fun toPageFailure(state: HistoryUiState, hasLoadedHistory: Boolean, isRefresh: Boolean): HistoryUiState =
        state.copy(
            presentation =
                if (hasLoadedHistory) state.presentation else HistoryPresentation.LOAD_ERROR,
            isRefreshing = false,
            isLoadingMore = false,
            hasRefreshError = isRefresh && hasLoadedHistory,
            hasLoadMoreError = !isRefresh
        )

    fun mergePending(
        history: HistoryUiState,
        completions: List<PendingPracticeCompletion>,
        discardId: String?
    ): HistoryUiState {
        val type = getPracticeType(history.selectedFilterId)
        val visible = completions.distinctBy { it.attemptId }.filter { type == null || it.practiceType == type }
        val pendingIds = visible.map { it.attemptId }.toSet()
        val groups =
            history.groups.mapNotNull { group ->
                group.copy(sessions = group.sessions.filter { it.id !in pendingIds }).takeIf {
                    it.sessions.isNotEmpty()
                }
            }
        return history.copy(
            presentation =
                if (history.presentation == HistoryPresentation.EMPTY && visible.isEmpty() && type != null)
                    HistoryPresentation.NO_MATCH
                else history.presentation,
            groups = groups,
            pendingDiscardId =
                discardId.takeIf { id -> visible.any { it.attemptId == id && canDiscard(it.status) } },
            pendingSessions =
                visible.map {
                    HistoryPendingSessionUiState(
                        id = it.attemptId,
                        title = it.scenarioTitle,
                        practiceType = it.practiceType,
                        startedAtMillis = it.startedAtEpochMillis,
                        status = it.status,
                        saveFailure = it.saveFailure
                    )
                }
        )
    }

    suspend fun toGroups(
        snapshot: List<PracticeAttempt>,
        scenarioTitles: Map<String, String>
    ): List<HistoryGroupUiState> {
        val titles = scenarioTitles.toMap()
        return withContext(Dispatchers.Default) {
            snapshot
                .groupBy { it.startedAt.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() }
                .map { (date, entries) ->
                    HistoryGroupUiState(
                        id = date.toString(),
                        startedAtMillis = entries.first().startedAt.time,
                        sessions =
                            entries.map { attempt ->
                                HistorySessionUiState(
                                    id = attempt.attemptId,
                                    title = titles[attempt.scenarioId].defaultEmpty(),
                                    result = HistoryResultMapper.toPresentation(attempt.result, attempt.practiceType),
                                    practiceType = attempt.practiceType,
                                    duration = attempt.durationInSeconds.toDurationLabel(),
                                    startedAtMillis = attempt.startedAt.time
                                )
                            }
                    )
                }
        }
    }

    fun getPracticeType(filterId: String): PracticeType? =
        when (filterId) {
            AppConstant.HISTORY_FILTER_LESSONS -> PracticeType.TRAINING_UNIT
            AppConstant.HISTORY_FILTER_GOALS -> PracticeType.GOAL_ASSESSMENT
            AppConstant.HISTORY_FILTER_FREE_PRACTICE -> PracticeType.FREE_PRACTICE
            else -> null
        }

    fun defaultFilters() =
        listOf(
            ContentFilterUiState(AppConstant.HISTORY_FILTER_ALL, ""),
            ContentFilterUiState(AppConstant.HISTORY_FILTER_LESSONS, ""),
            ContentFilterUiState(AppConstant.HISTORY_FILTER_GOALS, ""),
            ContentFilterUiState(AppConstant.HISTORY_FILTER_FREE_PRACTICE, "")
        )

    fun canDiscard(status: PracticeCompletionStatus): Boolean =
        status == PracticeCompletionStatus.AWAITING_GRADING || status == PracticeCompletionStatus.SAVE_FAILED
}
