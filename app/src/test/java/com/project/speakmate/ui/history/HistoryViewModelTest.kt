package com.project.speakmate.ui.history

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.PendingPracticeCompletion
import com.project.speakmate.domain.model.PracticeAttempt
import com.project.speakmate.domain.model.PracticeCompletionStatus
import com.project.speakmate.domain.model.PracticeHistoryPage
import com.project.speakmate.domain.model.PracticeHistoryCursor
import com.project.speakmate.domain.model.PracticeType
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.repository.HistoryRepository
import com.project.speakmate.domain.repository.PracticeCompletionRepository
import com.project.speakmate.domain.repository.PracticeRepository
import com.project.speakmate.test.FakeAuthRepository
import com.project.speakmate.test.MainDispatcherRule
import com.project.speakmate.ui.history.state.HistoryNavigation
import com.project.speakmate.ui.history.state.HistoryPresentation
import com.project.speakmate.ui.history.state.HistoryResultPresentation
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Date
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val historyListFlow = MutableStateFlow<List<PracticeAttempt>>(emptyList())
    private val viewModelStore = ViewModelStore()
    private val pendingFlow = MutableStateFlow<List<PendingPracticeCompletion>>(emptyList())
    private lateinit var historyRepository: HistoryRepository
    private lateinit var practiceRepository: PracticeRepository
    private lateinit var viewModel: HistoryViewModel
    private lateinit var completionRepository: PracticeCompletionRepository

    private val now = Date(1700000000000L)
    private val later = Date(1700000065000L)

    private fun makeAttempt(id: String, type: PracticeType, result: AssessmentResult? = null) = PracticeAttempt(
        attemptId = id,
        scenarioId = "scenario_$id",
        practiceType = type,
        startedAt = now,
        endedAt = later,
        result = result,
        criteriaResult = null,
        requiredActionResults = emptyMap(),
        transcripts = emptyList(),
        feedbackSummary = null
    )

    @Before
    fun setUp() {
        historyRepository = mockk(relaxed = true)
        practiceRepository = mockk(relaxed = true)
        completionRepository = mockk(relaxed = true)
        every { completionRepository.observePendingCompletions() } returns pendingFlow
        coEvery { historyRepository.getPracticeHistoryPage(any(), any()) } coAnswers {
            val type = firstArg<PracticeType?>()
            Result.success(PracticeHistoryPage(historyListFlow.value.filter { type == null || it.practiceType == type }))
        }
        coEvery { practiceRepository.getScenario(any()) } returns Result.failure(RuntimeException("Not found"))
        viewModel = HistoryViewModel(historyRepository, practiceRepository, completionRepository, FakeAuthRepository())
        viewModelStore.put(HistoryViewModel::class.java.name, viewModel)
        viewModel.onStarted()
    }

    @After
    fun clearViewModel() = runTest {
        if (!::viewModel.isInitialized) return@runTest
        val job = viewModel.viewModelScope.coroutineContext[Job]
        viewModelStore.clear()
        job?.join()
    }

    @Test
    fun retryRestartsPendingObserverAfterFailure() = runTest {
        viewModelStore.clear()
        var subscriptions = 0
        every { completionRepository.observePendingCompletions() } answers {
            flow {
                subscriptions++
                if (subscriptions == 1) throw IllegalStateException()
                emitAll(pendingFlow)
            }
        }
        viewModel = HistoryViewModel(historyRepository, practiceRepository, completionRepository, FakeAuthRepository())
        viewModelStore.put(HistoryViewModel::class.java.name, viewModel)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(HistoryPresentation.LOAD_ERROR, viewModel.uiState.value.presentation)
        viewModel.retry()
        advanceUntilIdle()
        viewModel.onStarted()
        advanceUntilIdle()
        assertEquals(2, subscriptions)
        assertEquals(HistoryPresentation.EMPTY, viewModel.uiState.first { it.presentation != HistoryPresentation.LOADING }.presentation)
    }

    @Test
    fun initialStateIsLoading() {
        assertEquals(HistoryPresentation.LOADING, viewModel.uiState.value.presentation)
    }

    @Test
    fun emptyListResultsInEmptyPresentation() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        historyListFlow.value = emptyList()
        viewModel.retry()
        advanceUntilIdle()

        val state = viewModel.uiState.first { it.presentation != HistoryPresentation.LOADING }
        assertEquals(HistoryPresentation.EMPTY, state.presentation)
        assertTrue(state.groups.isEmpty())
        assertFalse(state.filters.isEmpty())
    }

    @Test
    fun populatedListGroupsByDateAndMapsResults() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        val attempts = listOf(
            makeAttempt("1", PracticeType.FREE_PRACTICE, null),
            makeAttempt("2", PracticeType.TRAINING_UNIT, AssessmentResult.PASS),
            makeAttempt("3", PracticeType.TRAINING_UNIT, AssessmentResult.FAIL)
        )
        historyListFlow.value = attempts
        viewModel.retry()
        advanceUntilIdle()

        val state = viewModel.uiState.first { it.presentation == HistoryPresentation.POPULATED }
        assertEquals(HistoryPresentation.POPULATED, state.presentation)
        assertEquals(1, state.groups.size)
        assertEquals(3, state.groups[0].sessions.size)

        val passSession = state.groups[0].sessions.first { it.id == "2" }
        assertEquals(HistoryResultPresentation.PASSED, passSession.result)
        assertEquals(PracticeType.TRAINING_UNIT, passSession.practiceType)

        val completedSession = state.groups[0].sessions.first { it.id == "1" }
        assertEquals(HistoryResultPresentation.COMPLETED, completedSession.result)
        assertEquals(PracticeType.FREE_PRACTICE, completedSession.practiceType)
    }

    @Test
    fun scenarioTitleResolvesToScenarioNameWhenAvailable() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        val scenario = Scenario("scenario_1", "Gọi món tại nhà hàng", "Description", "Prompt")
        coEvery { practiceRepository.getScenario("scenario_1") } returns Result.success(scenario)

        val attempts = listOf(makeAttempt("1", PracticeType.FREE_PRACTICE, null))
        historyListFlow.value = attempts
        viewModel.retry()
        advanceUntilIdle()

        val state = viewModel.uiState.first { it.groups.flatMap { group -> group.sessions }.any { it.title == "Gọi món tại nhà hàng" } }
        val session = state.groups[0].sessions.first()
        assertEquals("Gọi món tại nhà hàng", session.title)
    }

    @Test
    fun defaultFiltersHasThreeFilters() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        historyListFlow.value = emptyList()
        viewModel.retry()
        advanceUntilIdle()

        val state = viewModel.uiState.first { it.presentation != HistoryPresentation.LOADING }
        assertEquals(4, state.filters.size)
        assertEquals("all", state.filters[0].id)
        assertEquals("training_unit", state.filters[1].id)
        assertEquals("goal_assessment", state.filters[2].id)
        assertEquals("free_practice", state.filters[3].id)
    }

    @Test
    fun filterChangesVisibleSessions() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        val attempts = listOf(
            makeAttempt("1", PracticeType.FREE_PRACTICE, null),
            makeAttempt("2", PracticeType.TRAINING_UNIT, AssessmentResult.PASS),
            makeAttempt("3", PracticeType.GOAL_ASSESSMENT, AssessmentResult.FAIL)
        )
        historyListFlow.value = attempts
        viewModel.retry()
        advanceUntilIdle()

        viewModel.selectFilter("training_unit")
        viewModel.retry()
        advanceUntilIdle()

        val state = viewModel.uiState.first { it.selectedFilterId == "training_unit" && it.presentation == HistoryPresentation.POPULATED }
        assertEquals("training_unit", state.selectedFilterId)
        assertEquals(1, state.groups.size)
        assertEquals(1, state.groups[0].sessions.size)
        assertTrue(state.groups[0].sessions.all { it.id == "2" })
    }
    @Test
    fun nullResultDistinguishesAssessmentsFromFreePracticeAndKeepsCallStartTime() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        historyListFlow.value = listOf(
            makeAttempt("free", PracticeType.FREE_PRACTICE),
            makeAttempt("unit", PracticeType.TRAINING_UNIT),
            makeAttempt("goal", PracticeType.GOAL_ASSESSMENT)
        )
        viewModel.retry()
        advanceUntilIdle()
        val sessions = viewModel.uiState.first { it.groups.flatMap { group -> group.sessions }.size == 3 }.groups.flatMap { it.sessions }
        assertEquals(HistoryResultPresentation.COMPLETED, sessions[0].result)
        assertEquals(HistoryResultPresentation.NOT_EVALUATED, sessions[1].result)
        assertEquals(HistoryResultPresentation.NOT_EVALUATED, sessions[2].result)
        assertEquals(now.time, sessions[0].startedAtMillis)
        assertEquals("01:05", sessions[0].duration)
        viewModel.selectFilter("training_unit")
        viewModel.retry()
        advanceUntilIdle()
        val filtered = viewModel.uiState.first { it.selectedFilterId == "training_unit" && it.presentation == HistoryPresentation.POPULATED }
        assertEquals(setOf("unit"), filtered.groups.flatMap { it.sessions }.map { it.id }.toSet())
    }

    @Test
    fun pendingCompletionsRemainVisibleWhenSavedHistoryIsEmptyOrFails() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        pendingFlow.value = listOf(pending("free", PracticeType.FREE_PRACTICE, PracticeCompletionStatus.SAVE_FAILED))
        viewModel.retry()
        advanceUntilIdle()
        assertEquals(HistoryPresentation.EMPTY, viewModel.uiState.first {
            it.presentation == HistoryPresentation.EMPTY && it.pendingSessions.isNotEmpty()
        }.presentation)
        assertEquals(listOf("free"), viewModel.uiState.value.pendingSessions.map { it.id })

        coEvery { historyRepository.getPracticeHistoryPage(any(), any()) } returns Result.failure(IllegalStateException())
        viewModel.retry()
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(HistoryPresentation.EMPTY, viewModel.uiState.value.presentation)
        assertTrue(viewModel.uiState.first { it.hasRefreshError }.hasRefreshError)
        assertEquals(listOf("free"), viewModel.uiState.value.pendingSessions.map { it.id })
        viewModel.retrySave("free")
        verify(exactly = 1) { completionRepository.retrySave("free") }
    }

    @Test
    fun pendingTypeFiltersKeepUnitAndGoalTogetherAndHideDuplicateSavedAttempts() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        val free = pending("free", PracticeType.FREE_PRACTICE, PracticeCompletionStatus.SAVE_FAILED)
        pendingFlow.value = listOf(
            free, free,
            pending("unit", PracticeType.TRAINING_UNIT, PracticeCompletionStatus.GRADING),
            pending("goal", PracticeType.GOAL_ASSESSMENT, PracticeCompletionStatus.SAVING)
        )
        historyListFlow.value = listOf(makeAttempt("free", PracticeType.FREE_PRACTICE), makeAttempt("saved", PracticeType.TRAINING_UNIT))
        viewModel.retry()
        advanceUntilIdle()
        val pendingState = viewModel.uiState.first { it.pendingSessions.isNotEmpty() }
        assertEquals(setOf("free", "unit", "goal"), pendingState.pendingSessions.map { it.id }.toSet())
        assertEquals(listOf("saved"), viewModel.uiState.first { it.groups.flatMap { group ->
            group.sessions }.any { it.id == "saved" } }.groups.flatMap { it.sessions }.map { it.id })

        viewModel.selectFilter("training_unit")
        viewModel.retry()
        advanceUntilIdle()
        assertEquals(listOf("unit"), viewModel.uiState.first { it.selectedFilterId == "training_unit" }.pendingSessions.map { it.id })
        viewModel.selectFilter("free_practice")
        viewModel.retry()
        advanceUntilIdle()
        val freeState = viewModel.uiState.first { it.selectedFilterId == "free_practice" }
        assertEquals(listOf("free"), freeState.pendingSessions.map { it.id })
        assertTrue(freeState.groups.isEmpty())
    }

    @Test
    fun pendingStatusChangesDisableRetryAndOnlyFailedIdsReachTheRepository() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        pendingFlow.value = listOf(
            pending("grading", PracticeType.TRAINING_UNIT, PracticeCompletionStatus.GRADING),
            pending("saving", PracticeType.TRAINING_UNIT, PracticeCompletionStatus.SAVING),
            pending("failed", PracticeType.TRAINING_UNIT, PracticeCompletionStatus.SAVE_FAILED)
        )
        viewModel.retry()
        advanceUntilIdle()
        viewModel.uiState.first { it.pendingSessions.isNotEmpty() }
        viewModel.retrySave("missing")
        viewModel.retrySave("grading")
        viewModel.retrySave("saving")
        viewModel.retrySave("failed")
        verify(exactly = 1) { completionRepository.retrySave(any()) }
        pendingFlow.value = listOf(pending("failed", PracticeType.TRAINING_UNIT, PracticeCompletionStatus.SAVING))
        viewModel.retry()
        advanceUntilIdle()
        viewModel.uiState.first { it.pendingSessions.singleOrNull()?.status == PracticeCompletionStatus.SAVING }
        viewModel.retrySave("failed")
        verify(exactly = 1) { completionRepository.retrySave("failed") }

        pendingFlow.value = emptyList()
        viewModel.retry()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.first { it.pendingSessions.isEmpty() }.pendingSessions.isEmpty())
    }

    @Test
    fun slowScenarioMetadataDoesNotBlockPendingUpdatesOrFilters() = runTest {
        val gate = CompletableDeferred<Unit>()
        coEvery { practiceRepository.getScenario(any()) } coAnswers {
            gate.await()
            Result.success(Scenario("scenario", "Title", "Description", "Prompt"))
        }
        historyListFlow.value = listOf(makeAttempt("saved", PracticeType.TRAINING_UNIT))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        pendingFlow.value = listOf(pending("free", PracticeType.FREE_PRACTICE, PracticeCompletionStatus.SAVE_FAILED))
        viewModel.retry()
        advanceUntilIdle()
        assertEquals(HistoryPresentation.POPULATED, viewModel.uiState.first { it.presentation == HistoryPresentation.POPULATED && it.pendingSessions.isNotEmpty() }.presentation)
        assertEquals("free", viewModel.uiState.first { it.pendingSessions.isNotEmpty() }.pendingSessions.single().id)
        viewModel.selectFilter("training_unit")
        viewModel.retry()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.first { it.selectedFilterId == "training_unit" }.pendingSessions.isEmpty())
        viewModel.selectFilter("all")
        gate.complete(Unit)
        viewModel.retry()
        advanceUntilIdle()
        assertEquals("free", viewModel.uiState.first { it.pendingSessions.isNotEmpty() }.pendingSessions.single().id)
        assertEquals(HistoryPresentation.POPULATED, viewModel.uiState.first {
            it.selectedFilterId == "all" && it.presentation == HistoryPresentation.POPULATED
        }.presentation)
    }

    @Test
    fun writerConfirmationRemovesPendingAndShowsTheSavedAttempt() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        pendingFlow.value = listOf(pending("same", PracticeType.FREE_PRACTICE, PracticeCompletionStatus.SAVE_FAILED))
        historyListFlow.value = listOf(makeAttempt("same", PracticeType.FREE_PRACTICE))
        viewModel.retry()
        advanceUntilIdle()
        val pendingState = viewModel.uiState.first { it.pendingSessions.isNotEmpty() }
        assertTrue(pendingState.groups.isEmpty())
        assertEquals("same", pendingState.pendingSessions.single().id)
        pendingFlow.value = emptyList()
        viewModel.retry()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.first { it.pendingSessions.isEmpty() }.pendingSessions.isEmpty())
        assertEquals("same", viewModel.uiState.first { it.groups.flatMap { group -> group.sessions }.any { it.id == "same" } }.groups.single().sessions.single().id)
    }

    @Test
    fun startPracticeSetsPendingNavigationAndConsumeClearsItOnce() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        advanceUntilIdle()
        viewModel.uiState.first { !it.isRefreshing }

        viewModel.startPractice()
        assertEquals(
            HistoryNavigation.OPEN_TRAINING_PATH,
            viewModel.uiState.first { it.pendingNavigation == HistoryNavigation.OPEN_TRAINING_PATH }.pendingNavigation
        )
        assertEquals(HistoryNavigation.OPEN_TRAINING_PATH, viewModel.consumePendingNavigation())
        assertNull(viewModel.uiState.first { it.pendingNavigation == null }.pendingNavigation)
        assertNull(viewModel.consumePendingNavigation())

        viewModel.selectFilter("free_practice")
        viewModel.uiState.first { it.selectedFilterId == "free_practice" && !it.isRefreshing }
        viewModel.startPractice()
        assertEquals(
            HistoryNavigation.OPEN_PRACTICE,
            viewModel.uiState.first { it.pendingNavigation == HistoryNavigation.OPEN_PRACTICE }.pendingNavigation
        )
        assertEquals(HistoryNavigation.OPEN_PRACTICE, viewModel.consumePendingNavigation())
        assertNull(viewModel.uiState.first { it.pendingNavigation == null }.pendingNavigation)
        assertNull(viewModel.consumePendingNavigation())
    }

    @Test
    fun paginationMergesDuplicateAttemptsAndFilterResetsCursor() = runTest {
        val cursor = PracticeHistoryCursor("user-1", null, 1L, 0, "first")
        coEvery { historyRepository.getPracticeHistoryPage(null, null) } returns Result.success(
            PracticeHistoryPage(listOf(makeAttempt("first", PracticeType.FREE_PRACTICE)), cursor, true)
        )
        coEvery { historyRepository.getPracticeHistoryPage(null, cursor) } returns Result.success(
            PracticeHistoryPage(listOf(
                makeAttempt("first", PracticeType.FREE_PRACTICE),
                makeAttempt("second", PracticeType.TRAINING_UNIT)
            ))
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        viewModel.retry()
        viewModel.uiState.first { it.hasMore && !it.isRefreshing }
        viewModel.loadMore()
        val page = viewModel.uiState.first { it.groups.sumOf { group -> group.sessions.size } == 2 }
        assertEquals(listOf("first", "second"), page.groups.flatMap { it.sessions }.map { it.id })
        assertFalse(page.hasMore)
        viewModel.selectFilter("training_unit")
        val filtered = viewModel.uiState.first { it.selectedFilterId == "training_unit" && !it.isRefreshing }
        assertTrue(filtered.groups.isEmpty())
        io.mockk.coVerify { historyRepository.getPracticeHistoryPage(PracticeType.TRAINING_UNIT, null) }
    }

    @Test
    fun removedPendingRemainsSavingUntilRefreshAcknowledgesIt() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        pendingFlow.value = listOf(pending("bridge", PracticeType.FREE_PRACTICE, PracticeCompletionStatus.SAVING))
        viewModel.uiState.first { it.pendingSessions.isNotEmpty() }
        viewModel.uiState.first { !it.isRefreshing }
        val gate = CompletableDeferred<PracticeHistoryPage>()
        coEvery { historyRepository.getPracticeHistoryPage(any(), any()) } coAnswers { Result.success(gate.await()) }
        pendingFlow.value = emptyList()
        val bridge = viewModel.uiState.first { it.isRefreshing }
        assertEquals("bridge", bridge.pendingSessions.single().id)
        assertEquals(PracticeCompletionStatus.SAVING, bridge.pendingSessions.single().status)
        gate.complete(PracticeHistoryPage(listOf(makeAttempt("bridge", PracticeType.FREE_PRACTICE))))
        val saved = viewModel.uiState.first { !it.isRefreshing && it.pendingSessions.isEmpty() }
        assertEquals("bridge", saved.groups.single().sessions.single().id)
    }

    private fun pending(id: String, type: PracticeType, status: PracticeCompletionStatus) = PendingPracticeCompletion(
        attemptId = id,
        practiceType = type,
        scenarioTitle = "Pending $id",
        startedAtEpochMillis = now.time,
        status = status
    )
}
