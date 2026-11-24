package com.project.speakmate.ui.call

import androidx.lifecycle.ViewModelStore
import com.project.speakmate.domain.model.CallActivityContext
import com.project.speakmate.domain.model.CallInterruption
import com.project.speakmate.domain.model.CallOutcome
import com.project.speakmate.domain.model.CallSessionState
import com.project.speakmate.domain.model.CallState
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.repository.CallRepository
import com.project.speakmate.domain.repository.PracticeCompletionRepository
import com.project.speakmate.test.MainDispatcherRule
import com.project.speakmate.ui.call.state.CallDialog
import com.project.speakmate.ui.call.state.CallPresentation
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
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
class CallSessionMappingCharacterizationTest {
    companion object {
        private const val ATTEMPT_ID = "attempt"
        private const val SCENARIO_ID = "scenario"
    }

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val store = ViewModelStore()
    private val sessions = MutableStateFlow(CallSessionState())
    private val calls = mockk<CallRepository>(relaxed = true)
    private val completion = mockk<PracticeCompletionRepository>(relaxed = true)
    private val context = CallActivityContext.FreePractice(
        Scenario(scenarioId = SCENARIO_ID, name = "", description = "", promptTemplate = "")
    )
    private lateinit var viewModel: CallViewModel

    @Before
    fun setUp() {
        every { calls.sessionState } returns sessions
        every { calls.audioLevel } returns MutableStateFlow(0f)
        every { completion.completePractice(any()) } returns ATTEMPT_ID
        viewModel = CallViewModel(
            repository = calls,
            dictionaryManager = CallDictionaryManager(
                dictionaryRepository = mockk(relaxed = true),
                savedVocabularyRepository = mockk(relaxed = true),
                repository = calls
            ),
            completionRepository = completion
        )
        store.put(CallSessionMappingCharacterizationTest::class.java.name, viewModel)
    }

    @After
    fun clear() {
        store.clear()
    }

    @Test
    fun endConfirmationSurvivesAnActiveSessionUpdate() = runTest {
        startSession()
        viewModel.requestEndCall()
        sessions.value = sessions.value.copy(isMuted = true)
        runCurrent()

        assertEquals(CallDialog.EndConfirmation, viewModel.uiState.value.activeDialog)
        assertTrue(viewModel.uiState.value.isMuted)
    }

    @Test
    fun endConfirmationClosesWhenAnActiveSessionStartsFinalizing() = runTest {
        startSession()
        viewModel.requestEndCall()
        sessions.value = sessions.value.copy(isFinalizing = true)
        runCurrent()

        assertNull(viewModel.uiState.value.activeDialog)
        assertEquals(CallPresentation.FINALIZING, viewModel.uiState.value.presentation)
    }

    @Test
    fun enteringPauseClosesTheActiveConfirmationAndLaterPausedUpdatesKeepANewOne() = runTest {
        startSession()
        viewModel.requestEndCall()
        sessions.value = sessions.value.copy(state = CallState.PAUSED, interruption = CallInterruption.NETWORK)
        runCurrent()
        assertNull(viewModel.uiState.value.activeDialog)
        assertEquals(CallPresentation.RECONNECTING, viewModel.uiState.value.presentation)

        viewModel.requestEndCall()
        sessions.value = sessions.value.copy(isMuted = true)
        runCurrent()

        assertEquals(CallDialog.EndConfirmation, viewModel.uiState.value.activeDialog)
    }

    @Test
    fun titleDialogSurvivesAClosedSessionWhileTheTranscriptCloses() = runTest {
        startSession()
        viewModel.openTranscript()
        viewModel.showTitle()
        sessions.value = CallSessionState(state = CallState.ENDED, isClosed = true)
        runCurrent()

        assertEquals(CallDialog.Title, viewModel.uiState.value.activeDialog)
        assertFalse(viewModel.uiState.value.isTranscriptVisible)
        assertFalse(viewModel.uiState.value.isExitReady)
    }

    @Test
    fun exitBecomesReadyAfterCloseAndCannotBeRearmedAfterConsumption() = runTest {
        startSession()
        viewModel.exitCall()
        assertFalse(viewModel.uiState.value.isExitReady)

        sessions.value = CallSessionState(state = CallState.ENDED, isClosed = true)
        runCurrent()
        assertTrue(viewModel.uiState.value.isExitReady)
        assertTrue(viewModel.consumeExit())
        assertFalse(viewModel.consumeExit())

        sessions.value = sessions.value.copy(isMuted = true)
        runCurrent()

        assertFalse(viewModel.uiState.value.isExitReady)
        assertTrue(viewModel.uiState.value.hasConsumedExit)
    }

    @Test
    fun consumingAFinishedSessionPreventsReadinessFromRearmingButKeepsTheLatestSnapshot() = runTest {
        startSession()
        val finished = CallSessionState(state = CallState.ENDED, outcome = CallOutcome.COMPLETED)
        sessions.value = finished
        runCurrent()
        assertTrue(viewModel.uiState.value.isFinishedSessionReady)
        assertEquals(finished, viewModel.uiState.value.finishedSession)
        assertTrue(viewModel.consumeFinishedSession(hasCallResultConsumer = true))
        assertFalse(viewModel.consumeFinishedSession(hasCallResultConsumer = true))

        val updated = finished.copy(isClosed = true)
        sessions.value = updated
        runCurrent()
        assertFalse(viewModel.uiState.value.isFinishedSessionReady)
        assertEquals(updated, viewModel.uiState.value.finishedSession)

        sessions.value = CallSessionState()
        runCurrent()

        assertFalse(viewModel.uiState.value.isFinishedSessionReady)
        assertEquals(updated, viewModel.uiState.value.finishedSession)
    }

    @Test
    fun completionRunsOncePerStartBeforeTheTerminalSessionIsMapped() = runTest {
        startSession()
        every { completion.completePractice(any()) } answers {
            assertEquals(CallPresentation.LISTENING, viewModel.uiState.value.presentation)
            assertFalse(viewModel.uiState.value.isFinishedSessionReady)
            assertNull(viewModel.uiState.value.finishedSession)
            ATTEMPT_ID
        }
        sessions.value = CallSessionState(state = CallState.ENDED, outcome = CallOutcome.COMPLETED)
        runCurrent()
        sessions.value = sessions.value.copy(isClosed = true)
        runCurrent()

        verify(exactly = 1) { completion.completePractice(any()) }
        assertEquals(ATTEMPT_ID, viewModel.uiState.value.completionAttemptId)
        assertEquals(CallPresentation.FINALIZING, viewModel.uiState.value.presentation)
        assertTrue(viewModel.uiState.value.isFinishedSessionReady)
    }

    @Test
    fun completionFailureIsVisibleInTheSameTerminalSessionMappingWithoutAnAutomaticRetry() = runTest {
        startSession()
        every { completion.completePractice(any()) } throws IllegalStateException()
        sessions.value = CallSessionState(state = CallState.ENDED, outcome = CallOutcome.COMPLETED)
        runCurrent()
        sessions.value = sessions.value.copy(isClosed = true)
        runCurrent()

        assertTrue(viewModel.uiState.value.hasCompletionError)
        assertEquals(CallPresentation.FAILED, viewModel.uiState.value.presentation)
        verify(exactly = 1) { completion.completePractice(any()) }
    }

    @Test
    fun readyAndCallingIgnoreAnOutcomeUntilAnActiveSessionArrives() = runTest {
        runCurrent()
        viewModel.startCall(context)
        sessions.value = CallSessionState(state = CallState.READY, outcome = CallOutcome.CANCELLED)
        runCurrent()
        sessions.value = sessions.value.copy(state = CallState.CALLING)
        runCurrent()
        verify(exactly = 0) { completion.completePractice(any()) }

        sessions.value = sessions.value.copy(state = CallState.ACTIVE)
        runCurrent()

        verify(exactly = 1) { completion.completePractice(any()) }
    }

    @Test
    fun endedAndFailedSessionsTriggerCompletionEvenWithoutAnOutcome() = runTest {
        startSession()
        sessions.value = CallSessionState(state = CallState.ENDED)
        runCurrent()
        verify(exactly = 1) { completion.completePractice(any()) }

        viewModel.startCall(context)
        sessions.value = CallSessionState(state = CallState.FAILED)
        runCurrent()

        verify(exactly = 2) { completion.completePractice(any()) }
        assertFalse(viewModel.uiState.value.isFinishedSessionReady)
        assertNull(viewModel.uiState.value.finishedSession)
    }

    private fun TestScope.startSession() {
        runCurrent()
        viewModel.startCall(context)
        sessions.value = CallSessionState(state = CallState.ACTIVE)
        runCurrent()
    }
}
