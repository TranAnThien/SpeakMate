package com.project.speakmate.ui.history

import androidx.lifecycle.ViewModelStore
import com.project.speakmate.domain.model.PendingPracticeCompletion
import com.project.speakmate.domain.model.PracticeAttempt
import com.project.speakmate.domain.model.PracticeHistoryCursor
import com.project.speakmate.domain.model.PracticeHistoryPage
import com.project.speakmate.domain.model.PracticeType
import com.project.speakmate.domain.repository.HistoryRepository
import com.project.speakmate.domain.repository.PracticeCompletionRepository
import com.project.speakmate.domain.repository.PracticeRepository
import com.project.speakmate.test.FakeAuthRepository
import com.project.speakmate.test.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryPagingCharacterizationTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())
    private val store = ViewModelStore()
    private val history = mockk<HistoryRepository>()
    private val practice = mockk<PracticeRepository>()
    private val completion = mockk<PracticeCompletionRepository>()
    private val cursor = PracticeHistoryCursor(
        userId = "user-1",
        practiceType = null,
        startedAtSeconds = 1L,
        startedAtNanoseconds = 0,
        attemptId = "first"
    )
    private lateinit var viewModel: HistoryViewModel

    @Before
    fun setUp() {
        every { completion.observePendingCompletions() } returns
            MutableStateFlow<List<PendingPracticeCompletion>>(emptyList())
        coEvery { practice.getScenario(any()) } returns Result.failure(IllegalStateException())
        coEvery { history.getPracticeHistoryPage(null, null) } returns Result.success(
            PracticeHistoryPage(attempts = listOf(attempt("first")), nextCursor = cursor, hasMore = true)
        )
        viewModel = HistoryViewModel(
            historyRepository = history,
            practiceRepository = practice,
            completionRepository = completion,
            authRepository = FakeAuthRepository()
        )
        store.put("history", viewModel)
        viewModel.onStarted()
    }

    @After
    fun clear() {
        store.clear()
    }

    @Test
    fun repeatedCursorKeepsRowsAndReportsLoadMoreFailure() = runTest {
        collectLoadedPage()
        coEvery { history.getPracticeHistoryPage(null, cursor) } returns Result.success(
            PracticeHistoryPage(attempts = emptyList(), nextCursor = cursor, hasMore = true)
        )
        viewModel.loadMore()
        val state = viewModel.uiState.first { it.hasLoadMoreError }
        assertEquals(listOf("first"), state.groups.flatMap { it.sessions }.map { it.id })
        assertTrue(state.hasMore)
        assertFalse(state.isLoadingMore)
        assertFalse(state.hasRefreshError)
    }

    @Test
    fun emptyIntermediatePageAdvancesCursorAndKeepsLoadedRows() = runTest {
        collectLoadedPage()
        val next = cursor.copy(attemptId = "next")
        coEvery { history.getPracticeHistoryPage(null, cursor) } returns Result.success(
            PracticeHistoryPage(attempts = emptyList(), nextCursor = next, hasMore = true)
        )
        coEvery { history.getPracticeHistoryPage(null, next) } returns Result.success(
            PracticeHistoryPage(listOf(attempt("second")))
        )
        viewModel.loadMore()
        val intermediate = viewModel.uiState.first { it.hasLoadMoreError }
        assertTrue(intermediate.hasMore)
        assertEquals(listOf("first"), intermediate.groups.flatMap { it.sessions }.map { it.id })
        viewModel.loadMore()
        val state = viewModel.uiState.first { !it.hasMore && !it.isLoadingMore }
        assertEquals(listOf("first", "second"), state.groups.flatMap { it.sessions }.map { it.id })
        assertFalse(state.hasLoadMoreError)
    }

    @Test
    fun failedRefreshKeepsLoadedRowsAndSetsOnlyRefreshError() = runTest {
        collectLoadedPage()
        coEvery { history.getPracticeHistoryPage(null, null) } returns Result.failure(IllegalStateException())
        viewModel.retry()
        val state = viewModel.uiState.first { it.hasRefreshError }
        assertEquals(listOf("first"), state.groups.flatMap { it.sessions }.map { it.id })
        assertFalse(state.isRefreshing)
        assertFalse(state.hasLoadMoreError)
    }

    private suspend fun TestScope.collectLoadedPage() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        viewModel.uiState.first { it.hasMore && !it.isRefreshing }
    }

    private fun attempt(id: String) = PracticeAttempt(
        attemptId = id,
        scenarioId = "scenario",
        practiceType = PracticeType.FREE_PRACTICE,
        startedAt = Date(0L),
        endedAt = Date(0L),
        result = null,
        criteriaResult = null,
        requiredActionResults = emptyMap(),
        transcripts = emptyList(),
        feedbackSummary = null
    )
}
