package com.project.speakmate.ui.call

import com.project.speakmate.data.repository.PracticeCompletionRepositoryImpl
import com.project.speakmate.data.session.PracticeCompletionManager
import com.project.speakmate.data.session.PracticeCompletionTestFixtures
import com.project.speakmate.domain.model.AssessmentOutcome
import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.CallActivityContext
import com.project.speakmate.domain.model.CallActivityType
import com.project.speakmate.domain.model.CallFailure
import com.project.speakmate.domain.model.CallOutcome
import com.project.speakmate.domain.model.CallSessionState
import com.project.speakmate.domain.model.CallState
import com.project.speakmate.domain.model.DictionaryLookupResult
import com.project.speakmate.domain.model.InsufficientEvidenceReason
import com.project.speakmate.domain.model.LearningExample
import com.project.speakmate.domain.model.PracticeCompletion
import com.project.speakmate.domain.model.PracticeType
import com.project.speakmate.domain.model.RequiredAction
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
import com.project.speakmate.domain.rules.AssessmentRules
import com.project.speakmate.domain.rules.PracticeCompletionRules
import com.project.speakmate.test.FakeAuthRepository
import com.project.speakmate.test.MainDispatcherRule
import com.project.speakmate.ui.call.state.CallPresentation
import com.project.speakmate.ui.call.state.CallDictionaryStatus
import com.project.speakmate.ui.call.state.CallVocabularySaveStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.Clock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class CallViewModelTest {
    companion object {
        private const val EPOCH_START_MILLIS = 1_700_000_000_000L
        private const val ORDER_ACTION_ID = "order_drink"
    }


    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val operationFolder = TemporaryFolder()

    private val sessionStateFlow = MutableStateFlow(CallSessionState())
    private val audioLevelFlow = MutableStateFlow(0f)

    private lateinit var callRepository: CallRepository
    private lateinit var dictionaryRepository: DictionaryRepository
    private lateinit var savedVocabularyRepository: SavedVocabularyRepository
    private lateinit var historyRepository: HistoryRepository
    private lateinit var speakingAssessmentRepository: SpeakingAssessmentRepository

    private lateinit var viewModel: CallViewModel
    private lateinit var completionScope: CoroutineScope

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

        coEvery { historyRepository.saveCompletedPractice(any(), any()) } returns Result.success(Unit)
        completionScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val completionRepository = PracticeCompletionRepositoryImpl(
            PracticeCompletionManager(completionScope, FakeAuthRepository(), historyRepository, speakingAssessmentRepository, Clock.systemUTC())
        )

        viewModel = CallViewModel(
            repository = callRepository,
            dictionaryManager = CallDictionaryManager(dictionaryRepository, savedVocabularyRepository, callRepository),
            completionRepository = completionRepository
        )
    }

    @Test
    fun automaticStartIsConsumedOnceAcrossRouteRecreation() = runTest {
        assertTrue(viewModel.consumeStartReady())
        Assert.assertFalse(viewModel.consumeStartReady())
        Assert.assertFalse(viewModel.uiState.value.isExiting)
        Assert.assertFalse(viewModel.uiState.value.isStartReady)
    }

    @Test
    fun callEndedCanceledWhenUserDidNotSpeakDoesNotSaveHistory() = runTest {
        viewModel.startCall(trainingContext())

        sessionStateFlow.value = CallSessionState(
            state = CallState.ACTIVE,
            startedAtMillis = 1000L,
            hasUserSpeech = false,
            transcript = listOf(
                TranscriptMessage("1", SenderType.AI, "Hello! How can I help you?", 0, isFinal = true, isInterrupted = false)
            )
        )
        advanceUntilIdle()

        finishCall(isNetworkError = false)
        advanceUntilIdle()

        coVerify(exactly = 0) { historyRepository.saveCompletedPractice(any(), any()) }
    }

    @Test
    fun callEndedFailedDueToNetworkErrorDoesNotSaveHistory() = runTest {
        viewModel.startCall(trainingContext())

        sessionStateFlow.value = CallSessionState(
            state = CallState.ACTIVE,
            startedAtMillis = 1000L,
            hasUserSpeech = true,
            transcript = listOf(
                TranscriptMessage("1", SenderType.USER, "I want a coffee", 0, isFinal = true, isInterrupted = false)
            )
        )
        advanceUntilIdle()

        finishCall(isNetworkError = true)
        advanceUntilIdle()

        coVerify(exactly = 0) { historyRepository.saveCompletedPractice(any(), any()) }
    }

    @Test
    fun callEndedWhenAiServiceThrowsKeepsTheCompletedAttemptPending() = runTest {
        viewModel.startCall(trainingContext())

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

        finishCall(isNetworkError = false)
        runCurrent()

        coVerify(exactly = 1) { speakingAssessmentRepository.evaluate(any()) }
        coVerify(exactly = 0) { historyRepository.saveCompletedPractice(any(), any()) }
    }

    @Test
    fun callEndedNullResultWhenAiReturnsInsufficientEvidenceSavesAttemptWithNullResult() = runTest {
        viewModel.startCall(trainingContext())

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

        val attemptSlot = slot<PracticeCompletion>()
        coEvery { historyRepository.saveCompletedPractice(any(), capture(attemptSlot)) } returns Result.success(Unit)

        finishCall(isNetworkError = false)
        advanceUntilIdle()

        coVerify(exactly = 1) { historyRepository.saveCompletedPractice(any(), any()) }
        val savedAttempt = attemptSlot.captured.attempt
        assertNull(savedAttempt.result)
        assertNull(savedAttempt.criteriaResult)
    }

    @Test
    fun callEndedHappyPathSavesAttemptWithPassResult() = runTest {
        viewModel.startCall(trainingContext())

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

        val attemptSlot = slot<PracticeCompletion>()
        coEvery { historyRepository.saveCompletedPractice(any(), capture(attemptSlot)) } returns Result.success(Unit)

        finishCall(isNetworkError = false)
        advanceUntilIdle()

        coVerify(exactly = 1) { historyRepository.saveCompletedPractice(any(), any()) }
        val savedAttempt = attemptSlot.captured.attempt
        assertEquals(AssessmentResult.PASS, savedAttempt.result)
        assertEquals(sampleCriteriaResult, savedAttempt.criteriaResult)
        assertEquals(mapOf("order_drink" to true), savedAttempt.requiredActionResults)
        assertEquals(PracticeType.TRAINING_UNIT, savedAttempt.practiceType)
    }

    @Test
    fun callEndedFreePracticeDoesNotCallAiAssessmentAndSavesWithNullResult() = runTest {
        viewModel.startCall(CallActivityContext.FreePractice(testScenario))

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

        val attemptSlot = slot<PracticeCompletion>()
        coEvery { historyRepository.saveCompletedPractice(any(), capture(attemptSlot)) } returns Result.success(Unit)

        finishCall(isNetworkError = false)
        advanceUntilIdle()

        coVerify(exactly = 0) { speakingAssessmentRepository.evaluate(any()) }
        coVerify(exactly = 1) { historyRepository.saveCompletedPractice(any(), any()) }
        val savedAttempt = attemptSlot.captured.attempt
        assertEquals(PracticeType.FREE_PRACTICE, savedAttempt.practiceType)
        assertNull(savedAttempt.result)
        assertNull(savedAttempt.feedbackSummary)
    }

    @Test
    fun callEndedPlacementOrReassessmentDoesNotSavePracticeAttempt() = runTest {
        viewModel.startCall(CallActivityContext.Assessment(testScenario, CallActivityType.INITIAL_PLACEMENT, ""))

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

        finishCall(isNetworkError = false)
        advanceUntilIdle()

        coVerify(exactly = 0) { historyRepository.saveCompletedPractice(any(), any()) }
    }

    @Test
    fun calculateSpeakingResultReturnsFailWhenRequiredActionIsFalse() {
        val actions = mapOf("action_1" to true, "action_2" to false)
        val result = calculateResult(sampleCriteriaResult, actions)
        assertEquals(AssessmentResult.FAIL, result)
    }

    @Test
    fun calculateSpeakingResultBasedOnCriteriaMetCount() {
        val allMetActions = mapOf("action_1" to true)

        val fourMet = sampleCriteriaResult.copy(rangeMet = false)
        assertEquals(AssessmentResult.PASS, calculateResult(fourMet, allMetActions))

        val threeMet = sampleCriteriaResult.copy(rangeMet = false, accuracyMet = false)
        assertEquals(AssessmentResult.PARTIAL, calculateResult(threeMet, allMetActions))

        val twoMet = sampleCriteriaResult.copy(rangeMet = false, accuracyMet = false, fluencyMet = false)
        assertEquals(AssessmentResult.FAIL, calculateResult(twoMet, allMetActions))
    }

    @Test
    fun extractFinalTranscriptsFiltersNonFinalAndBlankTranscripts() {
        val raw = listOf(
            TranscriptMessage("1", SenderType.USER, "hello", 0, isFinal = false, isInterrupted = false),
            TranscriptMessage("2", SenderType.USER, "   ", 1, isFinal = true, isInterrupted = false),
            TranscriptMessage("3", SenderType.AI, "How can I help?", 2, isFinal = true, isInterrupted = false),
            TranscriptMessage("4", SenderType.USER, "A coffee please", 3, isFinal = true, isInterrupted = true)
        )

        val result = PracticeCompletionRules.getFinalTranscripts(raw)

        assertEquals(2, result.size)
        assertEquals("How can I help?", result[0].content)
        assertEquals(0, result[0].orderIndex)
        assertEquals("A coffee please", result[1].content)
        assertEquals(1, result[1].orderIndex)
        assertTrue(result[1].isInterrupted)
    }

    @Test
    fun lateVocabularySaveDoesNotChangeTheNewEntry() = runTest {
        viewModel.startCall(CallActivityContext.FreePractice(testScenario))
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

    @Test
    fun startCallDoesNotStickInConnectingWhenSessionRemainsReady() = runTest {
        viewModel.startCall(CallActivityContext.FreePractice(testScenario))
        advanceUntilIdle()

        assertEquals(CallPresentation.READY, viewModel.uiState.value.presentation)

        sessionStateFlow.value = CallSessionState(state = CallState.CALLING)
        advanceUntilIdle()

        assertEquals(CallPresentation.CONNECTING, viewModel.uiState.value.presentation)
    }

    @Test
    fun requestEndCallWhileReadyCancelsStartAndExits() = runTest {
        viewModel.startCall(CallActivityContext.FreePractice(testScenario))
        advanceUntilIdle()

        viewModel.requestEndCall()
        advanceUntilIdle()

        verify(exactly = 1) { callRepository.endCall() }
        verify(exactly = 1) { callRepository.close() }
        assertTrue(viewModel.uiState.value.isExiting)
    }
    @After
    fun tearDown() {
        completionScope.cancel()
    }

    @Test
    fun dictionaryLookupRetriesErrorsAndKeepsTheSelectedSentence() = runTest {
        openDictionarySession()
        coEvery { dictionaryRepository.lookUp("coffee", "Want coffee?") } returns
            DictionaryLookupResult.Error(IllegalStateException())
        viewModel.selectWord("message", 12, "coffee")
        advanceUntilIdle()
        assertEquals(CallDictionaryStatus.ERROR, viewModel.uiState.value.dictionary?.status)
        assertEquals("Want coffee?", viewModel.uiState.value.dictionary?.sentence)
        coEvery { dictionaryRepository.lookUp("coffee", "Want coffee?") } returns
            DictionaryLookupResult.NotFound
        viewModel.retryLookup()
        advanceUntilIdle()
        assertEquals(CallDictionaryStatus.NOT_FOUND, viewModel.uiState.value.dictionary?.status)
        viewModel.selectWord("message", -1, "coffee")
        assertEquals(CallDictionaryStatus.NOT_FOUND, viewModel.uiState.value.dictionary?.status)
    }

    @Test
    fun dictionarySavesAllRepositoryOutcomesAndFailure() = runTest {
        openDictionarySession()
        coEvery { dictionaryRepository.lookUp(any(), any()) } returns DictionaryLookupResult.Found(
            VocabularyItem("coffee", "meaning", "phonetic", "noun", LearningExample("example", "translation"))
        )
        val outcomes = listOf(
            Result.success(VocabularySaveResult.SAVED) to CallVocabularySaveStatus.SAVED,
            Result.success(VocabularySaveResult.MOVED_TO_TOP) to CallVocabularySaveStatus.MOVED_TO_TOP,
            Result.success(VocabularySaveResult.SAVED_REPLACING_OLDEST) to
                CallVocabularySaveStatus.SAVED_REPLACING_OLDEST,
            Result.failure<VocabularySaveResult>(IllegalStateException()) to CallVocabularySaveStatus.FAILED
        )
        outcomes.forEach { (outcome, expected) ->
            coEvery { savedVocabularyRepository.saveVocabulary(any()) } returns outcome
            viewModel.selectWord("message", 12, "coffee")
            advanceUntilIdle()
            viewModel.saveDictionaryWord()
            advanceUntilIdle()
            assertEquals(expected, viewModel.uiState.value.dictionary?.saveStatus)
        }
        coVerify(exactly = outcomes.size) { savedVocabularyRepository.saveVocabulary(any()) }
    }

    @Test
    fun closingDictionaryCancelsLookupAndStopsPronunciation() = runTest {
        openDictionarySession()
        var isCancelled = false
        val gate = CompletableDeferred<DictionaryLookupResult>()
        coEvery { dictionaryRepository.lookUp(any(), any()) } coAnswers {
            try {
                gate.await()
            } finally {
                isCancelled = true
            }
        }
        viewModel.selectWord("message", 12, "coffee")
        runCurrent()
        viewModel.closeDictionary()
        runCurrent()
        assertTrue(isCancelled)
        assertNull(viewModel.uiState.value.dictionary)
        verify(atLeast = 2) { callRepository.stopPronunciation() }
    }

    @Test
    fun dictionaryPronunciationTracksSessionAndClosesWithTranscript() = runTest {
        openDictionarySession()
        coEvery { dictionaryRepository.lookUp(any(), any()) } returns DictionaryLookupResult.Found(
            VocabularyItem("coffee", "meaning", "", "noun", LearningExample("", ""))
        )
        viewModel.selectWord("message", 12, "coffee")
        advanceUntilIdle()
        viewModel.playDictionaryWord()
        assertTrue(viewModel.uiState.value.dictionary?.isPronunciationLoading == true)
        verify(exactly = 1) { callRepository.playPronunciation("coffee") }
        sessionStateFlow.value = sessionStateFlow.value.copy(
            pronunciation = com.project.speakmate.domain.model.PronunciationState.PLAYING,
            pronunciationText = "coffee"
        )
        runCurrent()
        assertTrue(viewModel.uiState.value.dictionary?.isPronouncing == true)
        Assert.assertFalse(viewModel.uiState.value.dictionary?.isPronunciationLoading == true)
        viewModel.stopDictionaryWord()
        viewModel.closeTranscript()
        assertNull(viewModel.uiState.value.dictionary)
        Assert.assertFalse(viewModel.uiState.value.isTranscriptVisible)
    }

    @Test
    fun clearingCallViewModelClosesTheRepositoryWhileDictionaryLookupIsPending() = runTest {
        openDictionarySession()
        val gate = CompletableDeferred<DictionaryLookupResult>()
        coEvery { dictionaryRepository.lookUp(any(), any()) } coAnswers { gate.await() }
        viewModel.selectWord("message", 12, "coffee")
        val store = androidx.lifecycle.ViewModelStore()
        store.put("call", viewModel)
        store.clear()
        runCurrent()
        verify(exactly = 1) { callRepository.close() }
        assertTrue(viewModel.uiState.value.dictionary?.status == CallDictionaryStatus.LOADING)
    }

    private suspend fun openDictionarySession() {
        viewModel.startCall(CallActivityContext.FreePractice(testScenario))
        sessionStateFlow.value = CallSessionState(
            state = CallState.ACTIVE,
            transcript = listOf(TranscriptMessage(
                messageId = "message",
                senderType = SenderType.USER,
                content = "Hello! Want coffee? Yes.",
                orderIndex = 0,
                isFinal = true,
                isInterrupted = false
            ))
        )
        kotlinx.coroutines.yield()
        viewModel.openTranscript()
    }

    private fun trainingContext() = CallActivityContext.Training(
        PracticeCompletionTestFixtures.training.copy(
            scenario = testScenario,
            requiredActions = listOf(RequiredAction(ORDER_ACTION_ID, ORDER_ACTION_ID))
        )
    )

    private fun finishCall(isNetworkError: Boolean) {
        val session = sessionStateFlow.value
        sessionStateFlow.value = session.copy(
            state = if (isNetworkError) CallState.FAILED else CallState.ENDED,
            failure = if (isNetworkError) CallFailure.NETWORK else null,
            outcome = when {
                isNetworkError -> CallOutcome.FAILED
                session.hasUserSpeech -> CallOutcome.COMPLETED
                else -> CallOutcome.CANCELLED
            },
            startedAtEpochMillis = EPOCH_START_MILLIS
        )
    }

    private fun calculateResult(criteria: SpeakingCriteriaResult, actions: Map<String, Boolean>): AssessmentResult =
        AssessmentRules.calculateResult(criteria, actions.keys.map { RequiredAction(it, it) }, actions)

}
