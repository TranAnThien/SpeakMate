package com.project.speakmate.ui.placement

import androidx.lifecycle.ViewModelStore
import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.CallOutcome
import com.project.speakmate.domain.model.CallSessionState
import com.project.speakmate.domain.model.CallState
import com.project.speakmate.domain.model.PlacementResult
import com.project.speakmate.domain.model.PlacementStep
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingProgress
import com.project.speakmate.domain.model.TrainingStage
import com.project.speakmate.domain.repository.AssessmentTaskRepository
import com.project.speakmate.domain.repository.ScenarioRepository
import com.project.speakmate.domain.rules.PlacementRules
import com.project.speakmate.domain.rules.PlacementTestFixtures
import com.project.speakmate.test.FakeAuthRepository
import com.project.speakmate.test.FakeUserRepository
import com.project.speakmate.test.MainDispatcherRule
import com.project.speakmate.ui.placement.state.PlacementPhase
import io.mockk.coEvery
import io.mockk.mockk
import java.time.Clock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlacementViewModelTest {
    companion object {
        private const val NOW_MILLIS = 2_000L
        private const val AUDIO_ID = "audio"
        private const val VIEW_MODEL_KEY = "placement"
        private const val NAVIGATION_WAIT_MILLIS = 1_001L
    }

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val auth = FakeAuthRepository(PlacementTestFixtures.USER_ID)
    private val users = FakeUserRepository()
    private val checkpoints = FakePlacementCheckpointRepository(PlacementTestFixtures.getCheckpoint(PlacementStep.A1_CONFIRMATION))
    private val assessments = FakeSpeakingAssessmentRepository(PlacementTestFixtures.getOutcome(AssessmentResult.FAIL))
    private val tasks = mockk<AssessmentTaskRepository>()
    private val scenarios = mockk<ScenarioRepository>()
    private val clock = mockk<Clock>()

    @Test
    fun confirmedLevelIsSavedBeforeCheckpointIsCleared() = runTest {
        users.saveGate = CompletableDeferred()
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.resume()
        advanceUntilIdle()

        viewModel.submitCallResult(completedSession())
        runCurrent()

        assertTrue(viewModel.uiState.value.isSavingResult)
        assertNotNull(checkpoints.saved?.pendingResult)
        assertEquals(0, checkpoints.clearCalls)
        users.saveGate?.complete(Unit)
        advanceUntilIdle()

        assertEquals(SpeakingLevel.A1, users.savedLevel)
        assertEquals(PlacementTestFixtures.getOutcome(AssessmentResult.FAIL).criteria, users.savedCriteria)
        assertEquals(TrainingProgress(0, 0, TrainingStage.UNIT), users.savedProgress)
        assertEquals(NOW_MILLIS, users.savedAtMillis)
        assertNull(checkpoints.saved)
        assertTrue(viewModel.uiState.value.isResultSaved)
    }

    @Test
    fun saveFailureKeepsTheResultAndRetriesWithoutRegrading() = runTest {
        users.saveFailure = IllegalStateException()
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.resume()
        advanceUntilIdle()
        viewModel.submitCallResult(completedSession())
        advanceUntilIdle()
        val result = viewModel.uiState.value.result

        assertTrue(viewModel.uiState.value.saveFailed)
        assertNotNull(checkpoints.saved?.pendingResult)
        users.saveFailure = null
        viewModel.confirmResultSaved()
        advanceUntilIdle()

        assertEquals(1, assessments.evaluations)
        assertEquals(2, users.saveCalls)
        assertEquals(result, viewModel.uiState.value.result)
        assertTrue(viewModel.uiState.value.isResultSaved)
    }

    @Test
    fun reopeningAPendingResultSavesItWithoutAnotherCall() = runTest {
        val criteria = PlacementTestFixtures.getOutcome(AssessmentResult.PASS).criteria
        checkpoints.saved = requireNotNull(checkpoints.saved).copy(
            currentTaskId = null,
            pendingResult = PlacementResult(SpeakingLevel.A1, criteria, NOW_MILLIS, AssessmentResult.PASS)
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(PlacementPhase.RESULT, viewModel.uiState.value.phase)
        assertTrue(viewModel.uiState.value.isResultSaved)
        assertEquals(1, users.saveCalls)
        assertEquals(0, assessments.evaluations)
    }

    @Test
    fun repeatedSaveTapsDoNotCreateConcurrentWrites() = runTest {
        users.saveGate = CompletableDeferred()
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.resume()
        advanceUntilIdle()
        viewModel.submitCallResult(completedSession())
        runCurrent()

        viewModel.confirmResultSaved()
        viewModel.confirmResultSaved()
        runCurrent()
        assertEquals(1, users.saveCalls)
        users.saveGate?.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun skipMarksNotAssessedAndClearsCheckpointAfterSuccess() = runTest {
        checkpoints.saved = null
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.skip()
        advanceUntilIdle()

        assertEquals(1, users.skipCalls)
        assertEquals(1, checkpoints.clearCalls)
        assertEquals(0, users.saveCalls)
        assertFalse(viewModel.uiState.value.isSavingResult)
    }

    @Test
    fun skipFailurePreservesCheckpointAndAllowsAnotherAttempt() = runTest {
        users.saveFailure = IllegalStateException()
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.skip()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.saveFailed)
        assertEquals(0, checkpoints.clearCalls)

        users.saveFailure = null
        viewModel.skip()
        advanceUntilIdle()
        assertEquals(2, users.skipCalls)
        assertNull(checkpoints.saved)
    }

    @Test
    fun resultSubmittedAsThePreviousJobPublishesCallingIsGraded() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val observer = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.first { state ->
                if (state.phase == PlacementPhase.CALLING) viewModel.submitCallResult(completedSession())
                state.phase == PlacementPhase.RESULT
            }
        }
        viewModel.resume()
        advanceUntilIdle()

        assertEquals(1, assessments.evaluations)
        assertEquals(PlacementPhase.RESULT, viewModel.uiState.value.phase)
        observer.cancel()
    }

    @Test
    fun expiredCheckpointIsRejectedBeforeAssessment() = runTest {
        var nowMillis = NOW_MILLIS
        coEvery { clock.millis() } answers { nowMillis }
        val viewModel = createViewModel(shouldConfigureClock = false)
        advanceUntilIdle()
        viewModel.resume()
        advanceUntilIdle()
        nowMillis = PlacementTestFixtures.CREATED_AT_MILLIS + PlacementRules.CHECKPOINT_TTL_MILLIS

        viewModel.submitCallResult(completedSession())
        advanceUntilIdle()

        assertEquals(0, assessments.evaluations)
        assertEquals(0, users.saveCalls)
        assertEquals(PlacementStep.INITIAL_A2, checkpoints.saved?.step)
        assertEquals(listOf(AUDIO_ID), assessments.clearedAudioIds)
    }

    @Test
    fun checkpointExpiringDuringGradingDoesNotSaveALevel() = runTest {
        var nowMillis = NOW_MILLIS
        coEvery { clock.millis() } answers { nowMillis }
        assessments.gate = CompletableDeferred()
        val viewModel = createViewModel(shouldConfigureClock = false)
        advanceUntilIdle()
        viewModel.resume()
        advanceUntilIdle()
        viewModel.submitCallResult(completedSession())
        runCurrent()

        nowMillis = PlacementTestFixtures.CREATED_AT_MILLIS + PlacementRules.CHECKPOINT_TTL_MILLIS
        assessments.gate?.complete(Unit)
        advanceUntilIdle()

        assertEquals(0, users.saveCalls)
        assertEquals(PlacementStep.INITIAL_A2, checkpoints.saved?.step)
    }

    @Test
    fun leaveStopsWaitingWhenThereIsNoEffectCollector() = runTest {
        checkpoints.saved = null
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.exit()
        runCurrent()
        advanceTimeBy(NAVIGATION_WAIT_MILLIS)
        runCurrent()
        viewModel.start()
        advanceUntilIdle()

        assertEquals(PlacementPhase.CALLING, viewModel.uiState.value.phase)
    }

    @Test
    fun clearingTheViewModelCancelsGradingAndReleasesAudio() = runTest {
        assessments.gate = CompletableDeferred()
        val viewModel = createViewModel()
        val store = ViewModelStore()
        store.put(VIEW_MODEL_KEY, viewModel)
        advanceUntilIdle()
        viewModel.resume()
        advanceUntilIdle()
        viewModel.submitCallResult(completedSession())
        runCurrent()

        store.clear()
        advanceUntilIdle()

        assertEquals(listOf(AUDIO_ID), assessments.clearedAudioIds)
        assertEquals(0, users.saveCalls)
    }

    @Test
    fun leavingDuringGradingWithoutACollectorReturnsToAResumableState() = runTest {
        assessments.gate = CompletableDeferred()
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.resume()
        advanceUntilIdle()
        viewModel.submitCallResult(completedSession())
        runCurrent()

        viewModel.exit()
        advanceUntilIdle()

        assertEquals(PlacementPhase.RESUME_PROMPT, viewModel.uiState.value.phase)
        assertEquals(listOf(AUDIO_ID), assessments.clearedAudioIds)
        assertEquals(0, users.saveCalls)
    }

    private fun createViewModel(shouldConfigureClock: Boolean = true): PlacementViewModel {
        if (shouldConfigureClock) coEvery { clock.millis() } returns NOW_MILLIS
        coEvery { tasks.getAssessmentTasks() } returns PlacementTestFixtures.getTasks()
        coEvery { scenarios.getScenario(any()) } answers {
            Scenario(firstArg(), "Scenario", "Description", "Prompt")
        }
        return PlacementViewModel(auth, tasks, checkpoints, assessments, scenarios, clock, users)
    }

    private fun completedSession() = CallSessionState(
        state = CallState.ENDED,
        outcome = CallOutcome.COMPLETED,
        hasUserSpeech = true,
        assessmentAudioId = AUDIO_ID
    )
}
