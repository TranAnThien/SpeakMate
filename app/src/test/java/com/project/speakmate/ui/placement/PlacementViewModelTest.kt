package com.project.speakmate.ui.placement

import androidx.lifecycle.ViewModelStore
import com.project.speakmate.data.repository.AssessmentGradingRepositoryImpl
import com.project.speakmate.data.session.AssessmentGradingManager
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
import com.project.speakmate.domain.model.User
import com.project.speakmate.domain.repository.AssessmentTaskRepository
import com.project.speakmate.domain.repository.ScenarioRepository
import com.project.speakmate.domain.rules.PlacementRules
import com.project.speakmate.domain.rules.PlacementTestFixtures
import com.project.speakmate.domain.rules.PracticeCompletionRules
import com.project.speakmate.test.FakeAuthRepository
import com.project.speakmate.test.FakeUserRepository
import com.project.speakmate.test.MainDispatcherRule
import com.project.speakmate.ui.placement.state.PlacementNavigation
import com.project.speakmate.ui.placement.state.PlacementPhase
import io.mockk.coEvery
import io.mockk.mockk
import java.time.Clock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
        private const val OTHER_USER_ID = "other-user"
    }

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val auth = FakeAuthRepository(PlacementTestFixtures.USER_ID)
    private val users = FakeUserRepository(User(PlacementTestFixtures.USER_ID, "Sample", "sample@example.com", ""))
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
        runCurrent()
        val result = viewModel.uiState.value.result

        assertTrue(viewModel.uiState.value.saveFailed)
        assertNotNull(checkpoints.saved?.pendingResult)
        users.saveFailure = null
        viewModel.confirmResultSaved()
        runCurrent()

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
        assertNull(checkpoints.saved)
        assertEquals(PlacementPhase.INTRODUCTION, viewModel.uiState.value.phase)
        assertEquals(com.project.speakmate.domain.model.PracticeRecoveryReason.ASSESSMENT_EXPIRED, viewModel.uiState.value.recoveryProblem)
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
        assertNull(checkpoints.saved)
        assertEquals(PlacementPhase.INTRODUCTION, viewModel.uiState.value.phase)
        assertEquals(com.project.speakmate.domain.model.PracticeRecoveryReason.ASSESSMENT_EXPIRED, viewModel.uiState.value.recoveryProblem)
    }

    @Test
    fun exitPublishesPendingHomeNavigationAndConsumeClearsItOnce() = runTest {
        checkpoints.saved = null
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.exit()
        advanceUntilIdle()

        assertEquals(PlacementNavigation.HOME, viewModel.uiState.value.pendingNavigation)
        assertEquals(PlacementNavigation.HOME, viewModel.consumePendingNavigation())
        assertNull(viewModel.uiState.value.pendingNavigation)
        assertNull(viewModel.consumePendingNavigation())
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
    fun leavingDuringGradingPublishesPendingHomeNavigationAndReleasesAudio() = runTest {
        assessments.gate = CompletableDeferred()
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.resume()
        advanceUntilIdle()
        viewModel.submitCallResult(completedSession())
        runCurrent()

        viewModel.exit()
        advanceUntilIdle()

        assertEquals(PlacementNavigation.HOME, viewModel.uiState.value.pendingNavigation)
        assertEquals(listOf(AUDIO_ID), assessments.clearedAudioIds)
        assertEquals(0, users.saveCalls)
    }

    @Test
    fun skipAndAlreadyAssessedSetPendingHomeNavigation() = runTest {
        checkpoints.saved = null
        val skipViewModel = createViewModel()
        advanceUntilIdle()
        skipViewModel.skip()
        advanceUntilIdle()
        assertEquals(PlacementNavigation.HOME, skipViewModel.consumePendingNavigation())
        assertNull(skipViewModel.consumePendingNavigation())

        users.setUser(User(PlacementTestFixtures.USER_ID, "Sample", "sample@example.com", "", SpeakingLevel.A2))
        val assessedViewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(PlacementNavigation.HOME, assessedViewModel.consumePendingNavigation())
        assertNull(assessedViewModel.consumePendingNavigation())
    }

    @Test
    fun assessedAccountRestoredWithoutAResultCannotStartInitialPlacement() = runTest {
        users.setUser(User(PlacementTestFixtures.USER_ID, "Sample", "sample@example.com", "", SpeakingLevel.A2))
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(PlacementPhase.ALREADY_ASSESSED, viewModel.uiState.value.phase)
        assertNull(checkpoints.saved)
        viewModel.start()
        viewModel.skip()
        advanceUntilIdle()
        assertEquals(PlacementPhase.ALREADY_ASSESSED, viewModel.uiState.value.phase)
        assertEquals(0, users.skipCalls)
    }

    @Test
    fun alreadyAssessedProfileIsNotOverwrittenByARestoredPendingInitialResult() = runTest {
        users.setUser(User(PlacementTestFixtures.USER_ID, "Sample", "sample@example.com", "", SpeakingLevel.A2))
        val criteria = PlacementTestFixtures.getOutcome(AssessmentResult.PASS).criteria
        checkpoints.saved = requireNotNull(checkpoints.saved).copy(
            pendingResult = PlacementResult(SpeakingLevel.A1, criteria, NOW_MILLIS, AssessmentResult.PASS)
        )
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(PlacementPhase.ALREADY_ASSESSED, viewModel.uiState.value.phase)
        assertEquals(0, users.saveCalls)
        assertNull(checkpoints.saved)
    }

    @Test
    fun changingAccountsLoadsTheObservedOwnerWithoutStickingInLoading() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        users.setUser(User(OTHER_USER_ID, "Sample", "sample@example.com", ""))
        auth.signedInUserId.value = OTHER_USER_ID
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.phase != PlacementPhase.LOADING)
        assertEquals(0, users.saveCalls)
    }

    @Test
    fun leavingAfterSaveFailureBeforeResultExpiryDoesNotCrashOrChangeState() = runTest {
        users.saveFailure = IllegalStateException()
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.resume()
        advanceUntilIdle()
        viewModel.submitCallResult(completedSession())
        runCurrent()
        assertTrue(viewModel.uiState.value.saveFailed)

        viewModel.exit()
        runCurrent()
        testDispatcher.scheduler.advanceTimeBy(PracticeCompletionRules.AUDIO_RETENTION_MILLIS + 1L)
        advanceUntilIdle()

        assertEquals(PlacementPhase.RESULT, viewModel.uiState.value.phase)
        assertNull(viewModel.uiState.value.recoveryProblem)
        assertEquals(PlacementNavigation.HOME, viewModel.uiState.value.pendingNavigation)
    }

    @Test
    fun switchingAccountBeforeResultExpiryDoesNotCrash() = runTest {
        users.saveFailure = IllegalStateException()
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.resume()
        advanceUntilIdle()
        viewModel.submitCallResult(completedSession())
        runCurrent()
        assertTrue(viewModel.uiState.value.saveFailed)

        users.setUser(User(OTHER_USER_ID, "Sample", "sample@example.com", ""))
        auth.signedInUserId.value = OTHER_USER_ID
        runCurrent()
        testDispatcher.scheduler.advanceTimeBy(PracticeCompletionRules.AUDIO_RETENTION_MILLIS + 1L)
        advanceUntilIdle()

        assertEquals(PlacementPhase.INTRODUCTION, viewModel.uiState.value.phase)
        assertNull(viewModel.uiState.value.recoveryProblem)
    }

    @Test
    fun passingInitialTaskPublishesNextTaskAndGradesTheSameCallEvidence() = runTest {
        checkpoints.saved = PlacementTestFixtures.getCheckpoint(PlacementStep.INITIAL_A2)
        assessments.outcome = PlacementTestFixtures.getOutcome(AssessmentResult.PASS)
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.resume()
        advanceUntilIdle()
        val task = requireNotNull(viewModel.uiState.value.currentTask)
        val expiresAtMillis = requireNotNull(checkpoints.saved).expiresAtMillis
        val call = completedSession().copy(assessmentAudioDurationMillis = NOW_MILLIS)
        viewModel.submitCallResult(call)
        runCurrent()
        val evidence = assessments.evidence.single()
        assertEquals(task.targetLevel, evidence.targetLevel)
        assertEquals(task.communicativeGoal, evidence.communicativeGoal)
        assertEquals(task.taskId, evidence.taskId)
        assertEquals(PlacementTestFixtures.USER_ID, evidence.userId)
        assertEquals(expiresAtMillis, evidence.expiresAtMillis)
        assertEquals(call.outcome, evidence.callOutcome)
        assertEquals(call.assessmentAudioId, evidence.assessmentAudioId)
        assertEquals(call.assessmentAudioDurationMillis, evidence.audioDurationMillis)
        assertTrue(evidence.requiredActions.isEmpty())
        assertTrue(!evidence.operationId.isNullOrBlank())
        assertEquals(PlacementPhase.NEXT_TASK, viewModel.uiState.value.phase)
        assertEquals(PlacementStep.B1_PROBE.role, viewModel.uiState.value.taskRole)
        assertEquals(2, viewModel.uiState.value.currentTaskNumber)
        assertNull(viewModel.uiState.value.scenario)
        assertEquals(0, users.saveCalls)
    }

    @Test
    fun failedResultExpiresAtTheAudioRetentionLimitEvenWhenCheckpointLivesLonger() = runTest {
        users.saveFailure = IllegalStateException()
        checkpoints.saved = requireNotNull(checkpoints.saved).copy(
            expiresAtMillis = NOW_MILLIS + PracticeCompletionRules.AUDIO_RETENTION_MILLIS + 1L
        )
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.resume()
        advanceUntilIdle()
        viewModel.submitCallResult(completedSession())
        runCurrent()
        assertEquals(PlacementPhase.RESULT, viewModel.uiState.value.phase)
        testDispatcher.scheduler.advanceTimeBy(PracticeCompletionRules.AUDIO_RETENTION_MILLIS - 1L)
        runCurrent()
        assertEquals(PlacementPhase.RESULT, viewModel.uiState.value.phase)
        testDispatcher.scheduler.advanceTimeBy(1L)
        runCurrent()
        assertEquals(PlacementPhase.INTRODUCTION, viewModel.uiState.value.phase)
        assertEquals(
            com.project.speakmate.domain.model.PracticeRecoveryReason.ASSESSMENT_EXPIRED,
            viewModel.uiState.value.recoveryProblem
        )
        assertNull(checkpoints.saved)
    }

    private fun createViewModel(shouldConfigureClock: Boolean = true): PlacementViewModel {
        if (shouldConfigureClock) coEvery { clock.millis() } returns NOW_MILLIS
        coEvery { tasks.getAssessmentTasks() } returns PlacementTestFixtures.getTasks()
        coEvery { scenarios.getScenario(any(), requireServer = true) } answers {
            Scenario(firstArg(), "Scenario", "Description", "Prompt")
        }
        return PlacementViewModel(auth, tasks, checkpoints, AssessmentGradingRepositoryImpl(AssessmentGradingManager(
            CoroutineScope(Dispatchers.Main), assessments, auth, clock)), scenarios, clock, users)
    }

    private fun completedSession() = CallSessionState(
        state = CallState.ENDED,
        outcome = CallOutcome.COMPLETED,
        hasUserSpeech = true,
        assessmentAudioId = AUDIO_ID
    )
}
