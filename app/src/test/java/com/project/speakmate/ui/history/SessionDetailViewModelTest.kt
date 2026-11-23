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
import com.project.speakmate.test.SavedStateHandleRule
import com.project.speakmate.ui.components.AssessmentCriterionType.FLUENCY
import com.project.speakmate.ui.components.AssessmentCriterionType.RANGE
import com.project.speakmate.ui.history.state.HistoryResultPresentation.NOT_EVALUATED
import com.project.speakmate.ui.history.state.SessionDetailPresentation
import io.mockk.every
import io.mockk.coEvery
import io.mockk.mockk
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val savedStateHandleRule = SavedStateHandleRule()

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
        coEvery { historyRepository.getPracticeAttempt("attempt_123", requireServer = true) } returns Result.success(sampleAttempt)
        coEvery { historyRepository.getSessionMessages("attempt_123", requireServer = true) } returns Result.success(sampleAttempt.transcripts)
        coEvery { practiceRepository.getScenario(any()) } returns Result.failure(RuntimeException("Not found"))

        val savedStateHandle = SavedStateHandle(mapOf("attemptId" to "attempt_123"))
        val viewModel = createViewModel(savedStateHandle, historyRepository, practiceRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SessionDetailPresentation.COMPLETED, state.presentation)
        assertEquals("", state.title)
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
        coEvery { historyRepository.getPracticeAttempt("attempt_123", requireServer = true) } returns Result.success(sampleAttempt)
        coEvery { historyRepository.getSessionMessages("attempt_123", requireServer = true) } returns Result.success(sampleAttempt.transcripts)
        coEvery { practiceRepository.getScenario("ordering_coffee") } returns Result.success(scenario)

        val savedStateHandle = SavedStateHandle(mapOf("attemptId" to "attempt_123"))
        val viewModel = createViewModel(savedStateHandle, historyRepository, practiceRepository)
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
        coEvery { historyRepository.getPracticeAttempt("attempt_123", requireServer = true) } returns Result.success(attemptWithCriteria)
        coEvery { historyRepository.getSessionMessages("attempt_123", requireServer = true) } returns Result.success(sampleAttempt.transcripts)
        coEvery { practiceRepository.getScenario(any()) } returns Result.failure(RuntimeException("Not found"))

        val savedStateHandle = SavedStateHandle(mapOf("attemptId" to "attempt_123"))
        val viewModel = createViewModel(savedStateHandle, historyRepository, practiceRepository)
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
        coEvery { historyRepository.getPracticeAttempt("attempt_123", requireServer = true) } returns Result.failure(RuntimeException("Not found"))
        coEvery { historyRepository.getSessionMessages("attempt_123", requireServer = true) } returns Result.success(sampleAttempt.transcripts)
        coEvery { practiceRepository.getScenario(any()) } returns Result.failure(RuntimeException("Not found"))

        val savedStateHandle = SavedStateHandle(mapOf("attemptId" to "attempt_123"))
        val viewModel = createViewModel(savedStateHandle, historyRepository, practiceRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SessionDetailPresentation.LOAD_ERROR, state.presentation)
    }
    @Test
    fun detailHasAResultAndMarksOnlyInterruptedAiMessages() = runTest {
        val history = mockk<HistoryRepository>()
        val practice = mockk<PracticeRepository>()
        val attempt = sampleAttempt.copy(result = null, transcripts = sampleAttempt.transcripts.map { it.copy(isInterrupted = true) })
        coEvery { history.getPracticeAttempt(any(), requireServer = true) } returns Result.success(attempt)
        coEvery { history.getSessionMessages(any(), requireServer = true) } returns Result.success(attempt.transcripts)
        coEvery { practice.getScenario(any()) } returns Result.failure(IllegalStateException())
        val viewModel = createViewModel(SavedStateHandle(mapOf("attemptId" to "attempt_123")), history, practice)
        advanceUntilIdle()
        assertEquals(NOT_EVALUATED, viewModel.uiState.value.result)
        assertTrue(viewModel.uiState.value.messages[0].isInterrupted)
        assertEquals(false, viewModel.uiState.value.messages[1].isInterrupted)
        assertNull(viewModel.uiState.value.feedbackSummary)
        assertEquals(false, viewModel.uiState.value.isAssessed)
    }

    @Test
    fun loadSessionDetailUsesActionDescriptionWhenAvailable() = runTest {
        val history = mockk<HistoryRepository>()
        val practice = mockk<PracticeRepository>()
        val actionId = sampleAttempt.requiredActionResults.keys.single()
        val description = sampleAttempt.transcripts.first().content
        val attempt = sampleAttempt.copy(requiredActionDescriptions = mapOf(actionId to description))
        coEvery { history.getPracticeAttempt(attemptId = any(), requireServer = true) } returns Result.success(attempt)
        coEvery { history.getSessionMessages(attemptId = any(), requireServer = true) } returns
            Result.success(attempt.transcripts)
        coEvery { practice.getScenario(any()) } returns Result.failure(IllegalStateException())

        val viewModel = createViewModel(
            arguments = SavedStateHandle(mapOf("attemptId" to sampleAttempt.attemptId)),
            history = history,
            practice = practice
        )
        advanceUntilIdle()

        val action = viewModel.uiState.value.requiredActions.single()
        assertEquals(description, action.text)
        assertEquals(actionId, action.id)
    }

    @Test
    fun missingAttemptArgumentIsALoadErrorInsteadOfACrash() = runTest {
        val viewModel = createViewModel(SavedStateHandle(), mockk(), mockk())
        advanceUntilIdle()
        assertEquals(SessionDetailPresentation.LOAD_ERROR, viewModel.uiState.value.presentation)
    }

    private fun createViewModel(arguments: SavedStateHandle, history: HistoryRepository, practice: PracticeRepository): SessionDetailViewModel {
        val completion = mockk<com.project.speakmate.domain.repository.PracticeCompletionRepository>(relaxed = true)
        every { completion.observeCompletion(any()) } returns kotlinx.coroutines.flow.flowOf(com.project.speakmate.domain.model.PracticeCompletionState.Unavailable)
        return SessionDetailViewModel(arguments, history, practice, completion,
            mockk<com.project.speakmate.domain.repository.TrainingRepository>(relaxed = true), com.project.speakmate.test.FakeAuthRepository())
    }

}
