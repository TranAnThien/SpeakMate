package com.project.speakmate.ui.call

import com.project.speakmate.domain.model.AssessmentOutcome
import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.CallActivityType
import com.project.speakmate.domain.model.CallSessionState
import com.project.speakmate.domain.model.CallState
import com.project.speakmate.domain.model.DictionaryLookupResult
import com.project.speakmate.domain.model.InsufficientEvidenceReason
import com.project.speakmate.domain.model.LearningExample
import com.project.speakmate.domain.model.PracticeAttempt
import com.project.speakmate.domain.model.PracticeType
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.model.SenderType
import com.project.speakmate.domain.model.SpeakingCriteriaResult
import com.project.speakmate.domain.model.SpeakingEvaluation
import com.project.speakmate.domain.model.TranscriptMessage
import com.project.speakmate.domain.model.VocabularyItem
import com.project.speakmate.domain.model.VocabularySaveResult
import com.project.speakmate.domain.repository.CallRepository
import com.project.speakmate.domain.repository.DictionaryRepository
import com.project.speakmate.domain.repository.HistoryRepository
import com.project.speakmate.domain.repository.SavedVocabularyRepository
import com.project.speakmate.domain.repository.SpeakingAssessmentRepository
import com.project.speakmate.test.MainDispatcherRule
import com.project.speakmate.ui.call.state.CallVocabularySaveStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CallViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sessionStateFlow = MutableStateFlow(CallSessionState())
    private val audioLevelFlow = MutableStateFlow(0f)

    private lateinit var callRepository: CallRepository
    private lateinit var dictionaryRepository: DictionaryRepository
    private lateinit var savedVocabularyRepository: SavedVocabularyRepository
    private lateinit var historyRepository: HistoryRepository
    private lateinit var speakingAssessmentRepository: SpeakingAssessmentRepository

    private lateinit var viewModel: CallViewModel

    private val testScenario = Scenario(
        scenarioId = "cafe_ordering",
        name = "Ordering Coffee",
        description = "Order a drink at a coffee shop",
        promptTemplate = "Prompt"
    )

    private val sampleCriteriaResult = SpeakingCriteriaResult(
        rangeMet = true,
        accuracyMet = true,
        fluencyMet = true,
        interactionMet = true,
        coherenceMet = true
    )

    @Before
    fun setUp() {
        callRepository = mockk(relaxed = true)
        dictionaryRepository = mockk(relaxed = true)
        savedVocabularyRepository = mockk(relaxed = true)
        historyRepository = mockk(relaxed = true)
        speakingAssessmentRepository = mockk(relaxed = true)

        every { callRepository.sessionState } returns sessionStateFlow
        every { callRepository.audioLevel } returns audioLevelFlow

        coEvery { historyRepository.savePracticeAttempt(any()) } returns Result.success(Unit)

        viewModel = CallViewModel(
            repository = callRepository,
            dictionaryRepository = dictionaryRepository,
            savedVocabularyRepository = savedVocabularyRepository,
            historyRepository = historyRepository,
            speakingAssessmentRepository = speakingAssessmentRepository
        )
    }

    @Test
    fun callEndedCanceledWhenUserDidNotSpeakDoesNotSaveHistory() = runTest {
        viewModel.startCall(testScenario, CallActivityType.TRAINING_UNIT)

        sessionStateFlow.value = CallSessionState(
            state = CallState.ACTIVE,
            startedAtMillis = 1000L,
            hasUserSpeech = false,
            transcript = listOf(
                TranscriptMessage("1", SenderType.AI, "Hello! How can I help you?", 0, isFinal = true, isInterrupted = false)
            )
        )
        advanceUntilIdle()

        viewModel.onCallEnded(isNetworkError = false)
        advanceUntilIdle()

        coVerify(exactly = 0) { historyRepository.savePracticeAttempt(any()) }
    }

    @Test
    fun callEndedFailedDueToNetworkErrorDoesNotSaveHistory() = runTest {
        viewModel.startCall(testScenario, CallActivityType.TRAINING_UNIT)

        sessionStateFlow.value = CallSessionState(
            state = CallState.ACTIVE,
            startedAtMillis = 1000L,
            hasUserSpeech = true,
            transcript = listOf(
                TranscriptMessage("1", SenderType.USER, "I want a coffee", 0, isFinal = true, isInterrupted = false)
            )
        )
        advanceUntilIdle()

        viewModel.onCallEnded(isNetworkError = true)
        advanceUntilIdle()

        coVerify(exactly = 0) { historyRepository.savePracticeAttempt(any()) }
    }

    @Test
    fun callEndedNullResultWhenAiServiceThrowsExceptionStillSavesAttemptWithNullResult() = runTest {
        viewModel.startCall(testScenario, CallActivityType.TRAINING_UNIT)

        sessionStateFlow.value = CallSessionState(
            state = CallState.ACTIVE,
            startedAtMillis = 1000L,
            endedAtMillis = 5000L,
            hasUserSpeech = true,
            transcript = listOf(
                TranscriptMessage("1", SenderType.USER, "Can I get an iced latte please?", 0, isFinal = true, isInterrupted = false)
            )
        )
        advanceUntilIdle()

        coEvery { speakingAssessmentRepository.evaluate(any()) } throws RuntimeException("AI evaluation timeout")

        val attemptSlot = slot<PracticeAttempt>()
        coEvery { historyRepository.savePracticeAttempt(capture(attemptSlot)) } returns Result.success(Unit)

        viewModel.onCallEnded(isNetworkError = false)
        advanceUntilIdle()

        coVerify(exactly = 1) { historyRepository.savePracticeAttempt(any()) }
        val savedAttempt = attemptSlot.captured
        assertEquals("cafe_ordering", savedAttempt.scenarioId)
        assertEquals(PracticeType.TRAINING_UNIT, savedAttempt.practiceType)
        assertNull(savedAttempt.result)
        assertNull(savedAttempt.criteriaResult)
        assertNotNull(savedAttempt.feedbackSummary)
        assertTrue(savedAttempt.feedbackSummary!!.contains("chưa thể đánh giá"))
        assertEquals(1, savedAttempt.transcripts.size)
    }

    @Test
    fun callEndedNullResultWhenAiReturnsInsufficientEvidenceSavesAttemptWithNullResult() = runTest {
        viewModel.startCall(testScenario, CallActivityType.TRAINING_UNIT)

        sessionStateFlow.value = CallSessionState(
            state = CallState.ACTIVE,
            startedAtMillis = 1000L,
            endedAtMillis = 5000L,
            hasUserSpeech = true,
            transcript = listOf(
                TranscriptMessage("1", SenderType.USER, "Yes please.", 0, isFinal = true, isInterrupted = false)
            )
        )
        advanceUntilIdle()

        coEvery { speakingAssessmentRepository.evaluate(any()) } returns AssessmentOutcome.InsufficientEvidence(
            InsufficientEvidenceReason.NOT_ENOUGH_SPEECH
        )

        val attemptSlot = slot<PracticeAttempt>()
        coEvery { historyRepository.savePracticeAttempt(capture(attemptSlot)) } returns Result.success(Unit)

        viewModel.onCallEnded(isNetworkError = false)
        advanceUntilIdle()

        coVerify(exactly = 1) { historyRepository.savePracticeAttempt(any()) }
        val savedAttempt = attemptSlot.captured
        assertNull(savedAttempt.result)
        assertNull(savedAttempt.criteriaResult)
    }

    @Test
    fun callEndedHappyPathSavesAttemptWithPassResult() = runTest {
        viewModel.startCall(testScenario, CallActivityType.TRAINING_UNIT)

        sessionStateFlow.value = CallSessionState(
            state = CallState.ACTIVE,
            startedAtMillis = 1000L,
            endedAtMillis = 15000L,
            hasUserSpeech = true,
            transcript = listOf(
                TranscriptMessage("1", SenderType.USER, "I would like to order a large coffee with almond milk.", 0, isFinal = true, isInterrupted = false)
            )
        )
        advanceUntilIdle()

        val mockEvaluation = mockk<SpeakingEvaluation>(relaxed = true)
        coEvery { speakingAssessmentRepository.evaluate(any()) } returns AssessmentOutcome.Evaluated(
            result = AssessmentResult.PASS,
            criteria = sampleCriteriaResult,
            requiredActionResults = mapOf("order_drink" to true),
            evaluation = mockEvaluation
        )

        val attemptSlot = slot<PracticeAttempt>()
        coEvery { historyRepository.savePracticeAttempt(capture(attemptSlot)) } returns Result.success(Unit)

        viewModel.onCallEnded(isNetworkError = false)
        advanceUntilIdle()

        coVerify(exactly = 1) { historyRepository.savePracticeAttempt(any()) }
        val savedAttempt = attemptSlot.captured
        assertEquals(AssessmentResult.PASS, savedAttempt.result)
        assertEquals(sampleCriteriaResult, savedAttempt.criteriaResult)
        assertEquals(mapOf("order_drink" to true), savedAttempt.requiredActionResults)
        assertEquals(PracticeType.TRAINING_UNIT, savedAttempt.practiceType)
    }

    @Test
    fun callEndedFreePracticeDoesNotCallAiAssessmentAndSavesWithNullResult() = runTest {
        viewModel.startCall(testScenario, CallActivityType.FREE_PRACTICE)

        sessionStateFlow.value = CallSessionState(
            state = CallState.ACTIVE,
            startedAtMillis = 1000L,
            endedAtMillis = 8000L,
            hasUserSpeech = true,
            transcript = listOf(
                TranscriptMessage("1", SenderType.USER, "How is the weather today?", 0, isFinal = true, isInterrupted = false)
            )
        )
        advanceUntilIdle()

        val attemptSlot = slot<PracticeAttempt>()
        coEvery { historyRepository.savePracticeAttempt(capture(attemptSlot)) } returns Result.success(Unit)

        viewModel.onCallEnded(isNetworkError = false)
        advanceUntilIdle()

        coVerify(exactly = 0) { speakingAssessmentRepository.evaluate(any()) }
        coVerify(exactly = 1) { historyRepository.savePracticeAttempt(any()) }
        val savedAttempt = attemptSlot.captured
        assertEquals(PracticeType.FREE_PRACTICE, savedAttempt.practiceType)
        assertNull(savedAttempt.result)
        assertEquals("Luyện tập tự do không chấm điểm.", savedAttempt.feedbackSummary)
    }

    @Test
    fun callEndedPlacementOrReassessmentDoesNotSavePracticeAttempt() = runTest {
        viewModel.startCall(testScenario, CallActivityType.INITIAL_PLACEMENT)

        sessionStateFlow.value = CallSessionState(
            state = CallState.ACTIVE,
            startedAtMillis = 1000L,
            endedAtMillis = 20000L,
            hasUserSpeech = true,
            transcript = listOf(
                TranscriptMessage("1", SenderType.USER, "My name is John and I live in Hanoi.", 0, isFinal = true, isInterrupted = false)
            )
        )
        advanceUntilIdle()

        viewModel.onCallEnded(isNetworkError = false)
        advanceUntilIdle()

        coVerify(exactly = 0) { historyRepository.savePracticeAttempt(any()) }
    }

    @Test
    fun calculateSpeakingResultReturnsFailWhenRequiredActionIsFalse() {
        val actions = mapOf("action_1" to true, "action_2" to false)
        val result = viewModel.calculateSpeakingResult(sampleCriteriaResult, actions)
        assertEquals(AssessmentResult.FAIL, result)
    }

    @Test
    fun calculateSpeakingResultBasedOnCriteriaMetCount() {
        val allMetActions = mapOf("action_1" to true)

        val fourMet = sampleCriteriaResult.copy(rangeMet = false)
        assertEquals(AssessmentResult.PASS, viewModel.calculateSpeakingResult(fourMet, allMetActions))

        val threeMet = sampleCriteriaResult.copy(rangeMet = false, accuracyMet = false)
        assertEquals(AssessmentResult.PARTIAL, viewModel.calculateSpeakingResult(threeMet, allMetActions))

        val twoMet = sampleCriteriaResult.copy(rangeMet = false, accuracyMet = false, fluencyMet = false)
        assertEquals(AssessmentResult.FAIL, viewModel.calculateSpeakingResult(twoMet, allMetActions))
    }

    @Test
    fun extractFinalTranscriptsFiltersNonFinalAndBlankTranscripts() {
        val raw = listOf(
            TranscriptMessage("1", SenderType.USER, "hello", 0, isFinal = false, isInterrupted = false),
            TranscriptMessage("2", SenderType.USER, "   ", 1, isFinal = true, isInterrupted = false),
            TranscriptMessage("3", SenderType.AI, "How can I help?", 2, isFinal = true, isInterrupted = false),
            TranscriptMessage("4", SenderType.USER, "A coffee please", 3, isFinal = true, isInterrupted = true)
        )

        val result = viewModel.extractFinalTranscripts(raw)

        assertEquals(2, result.size)
        assertEquals("How can I help?", result[0].content)
        assertEquals(0, result[0].orderIndex)
        assertEquals("A coffee please", result[1].content)
        assertEquals(1, result[1].orderIndex)
        assertTrue(result[1].isInterrupted)
    }

    @Test
    fun lateVocabularySaveDoesNotChangeTheNewEntry() = runTest {
        val earlierSave = CompletableDeferred<Result<VocabularySaveResult>>()
        val currentSave = CompletableDeferred<Result<VocabularySaveResult>>()
        coEvery { dictionaryRepository.lookUp(any(), any()) } coAnswers {
            DictionaryLookupResult.Found(
                VocabularyItem(firstArg<String>(), "meaning", "", "noun", LearningExample("", ""))
            )
        }
        coEvery { savedVocabularyRepository.saveVocabulary(match { it.word == "coffee" }) } coAnswers {
            earlierSave.await()
        }
        coEvery { savedVocabularyRepository.saveVocabulary(match { it.word == "bread" }) } coAnswers {
            currentSave.await()
        }
        sessionStateFlow.value = CallSessionState(
            state = CallState.ACTIVE,
            transcript = listOf(
                TranscriptMessage("one", SenderType.USER, "coffee", 0, isFinal = true, isInterrupted = false),
                TranscriptMessage("two", SenderType.USER, "bread", 1, isFinal = true, isInterrupted = false)
            )
        )
        advanceUntilIdle()

        viewModel.selectWord("one", 0, "coffee")
        advanceUntilIdle()
        viewModel.saveDictionaryWord()

        viewModel.selectWord("two", 0, "bread")
        advanceUntilIdle()
        viewModel.saveDictionaryWord()
        earlierSave.complete(Result.success(VocabularySaveResult.SAVED))
        advanceUntilIdle()

        val dictionary = requireNotNull(viewModel.uiState.value.dictionary)
        assertEquals("bread", dictionary.word)
        assertEquals(CallVocabularySaveStatus.SAVING, dictionary.saveStatus)

        currentSave.complete(Result.success(VocabularySaveResult.MOVED_TO_TOP))
        advanceUntilIdle()
        assertEquals(CallVocabularySaveStatus.MOVED_TO_TOP, viewModel.uiState.value.dictionary?.saveStatus)
    }
}
