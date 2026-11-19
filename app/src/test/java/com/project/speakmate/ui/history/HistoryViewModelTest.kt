package com.project.speakmate.ui.history

import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.PracticeAttempt
import com.project.speakmate.domain.model.PracticeType
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.repository.HistoryRepository
import com.project.speakmate.domain.repository.PracticeRepository
import com.project.speakmate.test.MainDispatcherRule
import com.project.speakmate.ui.history.state.HistoryPresentation
import com.project.speakmate.ui.history.state.HistoryResultPresentation
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val historyListFlow = MutableStateFlow<List<PracticeAttempt>>(emptyList())
    private lateinit var historyRepository: HistoryRepository
    private lateinit var practiceRepository: PracticeRepository
    private lateinit var viewModel: HistoryViewModel

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
        every { historyRepository.observePracticeHistory() } returns historyListFlow
        coEvery { practiceRepository.getScenario(any()) } returns Result.failure(RuntimeException("Not found"))
        viewModel = HistoryViewModel(historyRepository, practiceRepository)
    }

    @Test
    fun initialStateIsLoading() {
        assertEquals(HistoryPresentation.LOADING, viewModel.uiState.value.presentation)
    }

    @Test
    fun emptyListResultsInEmptyPresentation() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        historyListFlow.value = emptyList()
        advanceUntilIdle()

        val state = viewModel.uiState.value
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
        advanceUntilIdle()

        val state = viewModel.uiState.value
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
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val session = state.groups[0].sessions.first()
        assertEquals("Gọi món tại nhà hàng", session.title)
    }

    @Test
    fun defaultFiltersHasThreeFilters() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        historyListFlow.value = emptyList()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.filters.size)
        assertEquals("all", state.filters[0].id)
        assertEquals("training_unit", state.filters[1].id)
        assertEquals("free_practice", state.filters[2].id)
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
        advanceUntilIdle()

        viewModel.selectFilter("training_unit")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("training_unit", state.selectedFilterId)
        assertEquals(1, state.groups.size)
        assertEquals(2, state.groups[0].sessions.size)
        assertTrue(state.groups[0].sessions.all { it.id == "2" || it.id == "3" })
    }
    @Test
    fun nullResultDistinguishesAssessmentsFromFreePracticeAndKeepsCallStartTime() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        historyListFlow.value = listOf(
            makeAttempt("free", PracticeType.FREE_PRACTICE),
            makeAttempt("unit", PracticeType.TRAINING_UNIT),
            makeAttempt("goal", PracticeType.GOAL_ASSESSMENT)
        )
        advanceUntilIdle()
        val sessions = viewModel.uiState.value.groups.flatMap { it.sessions }
        assertEquals(HistoryResultPresentation.COMPLETED, sessions[0].result)
        assertEquals(HistoryResultPresentation.NOT_EVALUATED, sessions[1].result)
        assertEquals(HistoryResultPresentation.NOT_EVALUATED, sessions[2].result)
        assertEquals(java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(now), sessions[0].performedAt)
        assertEquals("01:05", sessions[0].duration)
        viewModel.selectFilter("training_unit")
        advanceUntilIdle()
        assertEquals(setOf("unit", "goal"), viewModel.uiState.value.groups.flatMap { it.sessions }.map { it.id }.toSet())
    }

}
