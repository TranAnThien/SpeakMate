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
import com.project.speakmate.domain.repository.TrainingRepository
import com.project.speakmate.test.MainDispatcherRule
import com.project.speakmate.test.SavedStateHandleRule
import com.project.speakmate.ui.components.AssessmentCriterionType.FLUENCY
import com.project.speakmate.ui.components.AssessmentCriterionType.RANGE
import com.project.speakmate.ui.history.state.HistoryResultPresentation.NOT_EVALUATED
import com.project.speakmate.ui.history.state.SessionDetailPresentation
import io.mockk.coEvery
import io.mockk.mockk
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val savedStateHandleRule = SavedStateHandleRule()

    @Before
    fun setUp() {
        coEvery { trainingRepository.getTrainingPath(any()) } returns Result.failure(RuntimeException("Not found"))
    }

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

    private val trainingRepository: TrainingRepository = mockk(relaxed = true)

    @Test
    fun loadSessionDetailWithNullCriteriaResultProducesEmptyCriteriaList() = runTest {
        val historyRepository = mockk<HistoryRepository>()
        val practiceRepository = mockk<PracticeRepository>()
        coEvery { historyRepository.getSessionDetail("attempt_123") } returns Result.success(sampleAttempt)
        coEvery { practiceRepository.getScenario(any()) } returns Result.failure(RuntimeException("Not found"))

        val savedStateHandle = SavedStateHandle(mapOf("attemptId" to "attempt_123"))
        val viewModel = SessionDetailViewModel(savedStateHandle, historyRepository, practiceRepository, trainingRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SessionDetailPresentation.COMPLETED, state.presentation)
        assertEquals("ordering_coffee", state.title)
        assertTrue(state.criteria.isEmpty())
        assertEquals(1, state.requiredActions.size)
        assertEquals(2, state.messages.size)
        assertEquals("m1", state.messages[0].id)
        assertEquals(SenderType.AI, state.messages[0].sender)
        assertEquals("m2", state.messages[1].id)
        assertEquals(SenderType.USER, state.messages[1].sender)
    }

    @Test
    fun loadSessionDetailWithScenarioTitleResolvesToScenarioName() = runTest {
        val historyRepository = mockk<HistoryRepository>()
        val practiceRepository = mockk<PracticeRepository>()
        val scenario = Scenario("ordering_coffee", "Hỏi đường người địa phương", "Description", "Prompt")
        coEvery { historyRepository.getSessionDetail("attempt_123") } returns Result.success(sampleAttempt)
        coEvery { practiceRepository.getScenario("ordering_coffee") } returns Result.success(scenario)

        val savedStateHandle = SavedStateHandle(mapOf("attemptId" to "attempt_123"))
        val viewModel = SessionDetailViewModel(savedStateHandle, historyRepository, practiceRepository, trainingRepository)
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
        val viewModel = SessionDetailViewModel(savedStateHandle, historyRepository, practiceRepository, trainingRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SessionDetailPresentation.COMPLETED, state.presentation)
        assertEquals(5, state.criteria.size)
        assertTrue(state.criteria.first { it.criterion == RANGE }.isPassed)
        org.junit.Assert.assertFalse(state.criteria.first { it.criterion == FLUENCY }.isPassed)
    }

    @Test
    fun loadSessionDetailFailureSetsLoadErrorPresentation() = runTest {
        val historyRepository = mockk<HistoryRepository>()
        val practiceRepository = mockk<PracticeRepository>()
        coEvery { historyRepository.getSessionDetail("attempt_123") } returns Result.failure(RuntimeException("Not found"))
        coEvery { practiceRepository.getScenario(any()) } returns Result.failure(RuntimeException("Not found"))

        val savedStateHandle = SavedStateHandle(mapOf("attemptId" to "attempt_123"))
        val viewModel = SessionDetailViewModel(savedStateHandle, historyRepository, practiceRepository, trainingRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SessionDetailPresentation.LOAD_ERROR, state.presentation)
    }
    @Test
    fun detailHasAResultAndMarksOnlyInterruptedAiMessages() = runTest {
        val history = mockk<HistoryRepository>()
        val practice = mockk<PracticeRepository>()
        val attempt = sampleAttempt.copy(result = null, transcripts = sampleAttempt.transcripts.map { it.copy(isInterrupted = true) })
        coEvery { history.getSessionDetail(any()) } returns Result.success(attempt)
        coEvery { practice.getScenario(any()) } returns Result.failure(IllegalStateException())
        val viewModel = SessionDetailViewModel(SavedStateHandle(mapOf("attemptId" to "attempt_123")), history, practice, trainingRepository)
        advanceUntilIdle()
        assertEquals(NOT_EVALUATED, viewModel.uiState.value.result)
        assertTrue(viewModel.uiState.value.messages[0].isInterrupted)
        assertEquals(false, viewModel.uiState.value.messages[1].isInterrupted)
        assertEquals("Tốt", viewModel.uiState.value.feedbackSummary)
    }

    @Test
    fun missingAttemptArgumentIsALoadErrorInsteadOfACrash() = runTest {
        val viewModel = SessionDetailViewModel(SavedStateHandle(), mockk(), mockk(), trainingRepository)
        advanceUntilIdle()
        assertEquals(SessionDetailPresentation.LOAD_ERROR, viewModel.uiState.value.presentation)
    }

    @Test
    fun loadSessionDetailUsesActionDescriptionWhenAvailable() = runTest {
        val historyRepository = mockk<HistoryRepository>()
        val practiceRepository = mockk<PracticeRepository>()
        val attemptWithDescription = sampleAttempt.copy(
            requiredActionResults = mapOf("A1-G01-U01-RA01" to true),
            requiredActionDescriptions = mapOf("A1-G01-U01-RA01" to "Chào hỏi và giới thiệu tên")
        )
        coEvery { historyRepository.getSessionDetail("attempt_123") } returns Result.success(attemptWithDescription)
        coEvery { practiceRepository.getScenario(any()) } returns Result.failure(RuntimeException("Not found"))

        val savedStateHandle = SavedStateHandle(mapOf("attemptId" to "attempt_123"))
        val viewModel = SessionDetailViewModel(savedStateHandle, historyRepository, practiceRepository, trainingRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Chào hỏi và giới thiệu tên", state.requiredActions[0].text)
        assertEquals("A1-G01-U01-RA01", state.requiredActions[0].id)
    }
}
