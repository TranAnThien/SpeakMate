package com.project.speakmate.ui.history

import androidx.lifecycle.SavedStateHandle
import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.PracticeAttempt
import com.project.speakmate.domain.model.PracticeType
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.model.SenderType
import com.project.speakmate.domain.model.SpeakingCriteriaResult
import com.project.speakmate.domain.model.TranscriptMessage
import com.project.speakmate.domain.repository.HistoryRepository
import com.project.speakmate.domain.repository.PracticeRepository
import com.project.speakmate.test.MainDispatcherRule
import com.project.speakmate.ui.history.state.SessionDetailPresentation
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class SessionDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sampleAttempt = PracticeAttempt(
        attemptId = "attempt_123",
        scenarioId = "ordering_coffee",
        practiceType = PracticeType.TRAINING_UNIT,
        startedAt = Date(1700000000000L),
        endedAt = Date(1700000060000L),
        result = AssessmentResult.PASS,
        criteriaResult = null,
        requiredActionResults = mapOf("order_drink" to true),
        transcripts = listOf(
            TranscriptMessage("m2", SenderType.USER, "An iced latte please", 1, isFinal = true, isInterrupted = false),
            TranscriptMessage("m1", SenderType.AI, "Hello! What can I get you?", 0, isFinal = true, isInterrupted = false)
        ),
        feedbackSummary = "Tốt"
    )

    @Test
    fun loadSessionDetailWithNullCriteriaResultProducesEmptyCriteriaList() = runTest {
        val historyRepository = mockk<HistoryRepository>()
        val practiceRepository = mockk<PracticeRepository>()
        coEvery { historyRepository.getSessionDetail("attempt_123") } returns Result.success(sampleAttempt)
        coEvery { practiceRepository.getScenario(any()) } returns Result.failure(RuntimeException("Not found"))

        val savedStateHandle = SavedStateHandle(mapOf("attemptId" to "attempt_123"))
        val viewModel = SessionDetailViewModel(savedStateHandle, historyRepository, practiceRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SessionDetailPresentation.COMPLETED, state.presentation)
        assertEquals("ordering_coffee", state.title)
        assertTrue(state.criteria.isEmpty())
        assertEquals(1, state.requiredActions.size)
        assertEquals(2, state.messages.size)
        assertEquals("m1", state.messages[0].id)
        assertEquals("AI", state.messages[0].metadata)
        assertEquals("m2", state.messages[1].id)
        assertEquals("Bạn", state.messages[1].metadata)
    }

    @Test
    fun loadSessionDetailWithScenarioTitleResolvesToScenarioName() = runTest {
        val historyRepository = mockk<HistoryRepository>()
        val practiceRepository = mockk<PracticeRepository>()
        val scenario = Scenario("ordering_coffee", "Hỏi đường người địa phương", "Description", "Prompt")
        coEvery { historyRepository.getSessionDetail("attempt_123") } returns Result.success(sampleAttempt)
        coEvery { practiceRepository.getScenario("ordering_coffee") } returns Result.success(scenario)

        val savedStateHandle = SavedStateHandle(mapOf("attemptId" to "attempt_123"))
        val viewModel = SessionDetailViewModel(savedStateHandle, historyRepository, practiceRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Hỏi đường người địa phương", state.title)
    }

    @Test
    fun loadSessionDetailWithCriteriaResultMapsAllFiveCriteria() = runTest {
        val attemptWithCriteria = sampleAttempt.copy(
            criteriaResult = SpeakingCriteriaResult(
                rangeMet = true,
                accuracyMet = true,
                fluencyMet = false,
                interactionMet = true,
                coherenceMet = true
            )
        )
        val historyRepository = mockk<HistoryRepository>()
        val practiceRepository = mockk<PracticeRepository>()
        coEvery { historyRepository.getSessionDetail("attempt_123") } returns Result.success(attemptWithCriteria)
        coEvery { practiceRepository.getScenario(any()) } returns Result.failure(RuntimeException("Not found"))

        val savedStateHandle = SavedStateHandle(mapOf("attemptId" to "attempt_123"))
        val viewModel = SessionDetailViewModel(savedStateHandle, historyRepository, practiceRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SessionDetailPresentation.COMPLETED, state.presentation)
        assertEquals(5, state.criteria.size)
        assertTrue(state.criteria.first { it.id == "range" }.isPassed)
        org.junit.Assert.assertFalse(state.criteria.first { it.id == "fluency" }.isPassed)
    }

    @Test
    fun loadSessionDetailFailureSetsLoadErrorPresentation() = runTest {
        val historyRepository = mockk<HistoryRepository>()
        val practiceRepository = mockk<PracticeRepository>()
        coEvery { historyRepository.getSessionDetail("attempt_123") } returns Result.failure(RuntimeException("Not found"))
        coEvery { practiceRepository.getScenario(any()) } returns Result.failure(RuntimeException("Not found"))

        val savedStateHandle = SavedStateHandle(mapOf("attemptId" to "attempt_123"))
        val viewModel = SessionDetailViewModel(savedStateHandle, historyRepository, practiceRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SessionDetailPresentation.LOAD_ERROR, state.presentation)
    }
}
