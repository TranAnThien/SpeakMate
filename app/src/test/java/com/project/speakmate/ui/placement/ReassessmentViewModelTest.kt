package com.project.speakmate.ui.placement

import androidx.lifecycle.ViewModelStore
import com.project.speakmate.domain.model.AssessmentOutcome
import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.CallOutcome
import com.project.speakmate.domain.model.CallSessionState
import com.project.speakmate.domain.model.CallState
import com.project.speakmate.domain.model.InsufficientEvidenceReason
import com.project.speakmate.domain.model.ReassessmentStep
import com.project.speakmate.domain.model.ReassessmentTransition
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingProgress
import com.project.speakmate.domain.model.TrainingStage
import com.project.speakmate.domain.rules.PlacementTestFixtures
import com.project.speakmate.domain.rules.ReassessmentTestFixtures
import com.project.speakmate.test.FakeAuthRepository
import com.project.speakmate.test.FakeUserRepository
import com.project.speakmate.test.MainDispatcherRule
import com.project.speakmate.ui.placement.state.ReassessmentPhase
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Date
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReassessmentViewModelTest {
    companion object {
        private const val NOW_MILLIS = 20_000L
        private const val AUDIO_ID = "reassessment-audio"
        private const val VIEW_MODEL_KEY = "reassessment"
    }

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())
    private val store = ViewModelStore()
    private val auth = FakeAuthRepository(ReassessmentTestFixtures.getUser(SpeakingLevel.A2).userId)
    private val users = FakeUserRepository(ReassessmentTestFixtures.getUser(SpeakingLevel.A2))
    private val tasks = FakeAssessmentTaskRepository()
    private val scenarios = FakeScenarioRepository()
    private val assessments = FakeSpeakingAssessmentRepository(PlacementTestFixtures.getOutcome(AssessmentResult.PASS))
    private val training = FakeTrainingRepository()
    private val clock = Clock.fixed(Instant.ofEpochMilli(NOW_MILLIS), ZoneOffset.UTC)

    @After
    fun clearViewModel() {
        store.clear()
    }

    @Test
    fun eligibleLevelsOpenIntroductionAndB2NeverStarts() = runTest {
        for (level in SpeakingLevel.entries) {
            users.setUser(ReassessmentTestFixtures.getUser(level))
            val viewModel = createViewModel()
            runCurrent()
            if (level in listOf(SpeakingLevel.A1, SpeakingLevel.A2, SpeakingLevel.B1)) {
                assertEquals(ReassessmentPhase.INTRODUCTION, viewModel.uiState.value.phase)
                viewModel.start()
                runCurrent()
                assertEquals(level, viewModel.uiState.value.assessment?.currentTask?.targetLevel)
            } else {
                assertTrue(viewModel.uiState.value.result is ReassessmentTransition.NotEligible)
                viewModel.start()
                runCurrent()
                assertNull(viewModel.uiState.value.scenario)
            }
            store.clear()
            runCurrent()
        }
        assertEquals(0, users.saveCalls)
    }

    @Test
    fun incompletePathCannotStartReassessment() = runTest {
        for (stage in listOf(TrainingStage.UNIT, TrainingStage.GOAL_ASSESSMENT)) {
            users.setUser(ReassessmentTestFixtures.getUser(SpeakingLevel.A2, TrainingProgress(0, 0, stage)))
            val viewModel = createViewModel()
            runCurrent()
            assertEquals(ReassessmentTransition.NotEligible(ReassessmentTransition.EligibilityReason.PATH_NOT_COMPLETED), viewModel.uiState.value.result)
            store.clear()
            runCurrent()
        }
    }

    @Test
    fun invalidProgressCannotStartEvenWhenItsStageSaysCompleted() = runTest {
        users.setUser(ReassessmentTestFixtures.getUser(SpeakingLevel.A2, TrainingProgress(-1, 0, TrainingStage.PATH_COMPLETED)))
        val viewModel = createViewModel()
        runCurrent()
        assertEquals(ReassessmentPhase.ERROR, viewModel.uiState.value.phase)
        assertEquals(0, users.saveCalls)
    }

    @Test
    fun promotionWritesOnlyTheFinalConfirmationCriteriaAndInitializesTheNewPath() = runTest {
        val viewModel = start(this)
        assertEquals(ReassessmentStep.CURRENT_CONFIRMATION, viewModel.uiState.value.assessment?.step)
        passAndContinue(viewModel, this)
        assertEquals(0, users.saveCalls)
        val probeScenario = viewModel.uiState.value.scenario?.scenarioId
        assertEquals(SpeakingLevel.B1, viewModel.uiState.value.assessment?.currentTask?.targetLevel)
        passAndContinue(viewModel, this)
        assertEquals(0, users.saveCalls)
        assertEquals(ReassessmentStep.NEXT_CONFIRMATION, viewModel.uiState.value.assessment?.step)
        assertNotEquals(probeScenario, viewModel.uiState.value.scenario?.scenarioId)
        val finalCriteria = assessments.outcome as AssessmentOutcome.Evaluated
        assessments.outcome = finalCriteria.copy(criteria = finalCriteria.criteria.copy(rangeMet = false))
        viewModel.submitCallResult(session())
        runCurrent()
        assertTrue(viewModel.uiState.value.isSaved)
        assertEquals(1, users.saveCalls)
        assertEquals(SpeakingLevel.B1, users.savedLevel)
        assertEquals((assessments.outcome as AssessmentOutcome.Evaluated).criteria, users.savedCriteria)
        assertEquals(NOW_MILLIS, users.savedAtMillis)
        assertEquals(TrainingProgress(0, 0, TrainingStage.UNIT), users.savedProgress)
        assertEquals(listOf(SpeakingLevel.A2, SpeakingLevel.B1, SpeakingLevel.B1), assessments.evidence.map { it.targetLevel })
        assertTrue(assessments.evidence.all { it.requiredActions.isEmpty() })
        assertEquals(listOf(AUDIO_ID, AUDIO_ID, AUDIO_ID), assessments.clearedAudioIds)
    }

    @Test
    fun eachEligibleLevelPromotesOnlyToItsImmediateSuccessor() = runTest {
        for (level in listOf(SpeakingLevel.A1, SpeakingLevel.A2, SpeakingLevel.B1)) {
            users.setUser(ReassessmentTestFixtures.getUser(level))
            val viewModel = complete(this)
            assertTrue(viewModel.uiState.value.isSaved)
            assertEquals(level.getNextLevel(), users.savedLevel)
            store.clear()
            runCurrent()
        }
    }

    @Test
    fun partialOrFailAtAnyStepEndsWithoutWritingOrLoweringTheLevel() = runTest {
        for (step in ReassessmentStep.entries) {
            for (result in listOf(AssessmentResult.PARTIAL, AssessmentResult.FAIL)) {
                assessments.outcome = PlacementTestFixtures.getOutcome(AssessmentResult.PASS)
                val viewModel = start(this)
                repeat(step.ordinal) { passAndContinue(viewModel, this) }
                assessments.outcome = PlacementTestFixtures.getOutcome(result)
                viewModel.submitCallResult(session())
                runCurrent()
                assertEquals(ReassessmentTransition.NotPromoted(step, result), viewModel.uiState.value.result)
                assertEquals(SpeakingLevel.A2, viewModel.uiState.value.currentLevel)
                assertEquals(0, users.saveCalls)
                viewModel.retryTask()
                runCurrent()
                assertEquals(ReassessmentPhase.RESULT, viewModel.uiState.value.phase)
                store.clear()
                runCurrent()
            }
        }
    }

    @Test
    fun noSpeechAndOtherNullOutcomesShareTwoRetriesOnTheSameTask() = runTest {
        val viewModel = start(this)
        val task = viewModel.uiState.value.assessment?.currentTask
        for (attempt in 0..2) {
            assessments.outcome = AssessmentOutcome.InsufficientEvidence(if (attempt == 1) InsufficientEvidenceReason.EVALUATION_FAILED else InsufficientEvidenceReason.NO_USER_SPEECH)
            viewModel.submitCallResult(session().copy(hasUserSpeech = false, outcome = CallOutcome.CANCELLED))
            runCurrent()
            if (attempt < 2) {
                assertTrue(viewModel.uiState.value.result is ReassessmentTransition.RetryTask)
                viewModel.retryTask()
                runCurrent()
                assertEquals(task, viewModel.uiState.value.assessment?.currentTask)
            } else {
                assertEquals(ReassessmentTransition.Stopped(ReassessmentTransition.StopReason.TECHNICAL_RETRY_LIMIT), viewModel.uiState.value.result)
                viewModel.retryTask()
                runCurrent()
                assertEquals(ReassessmentPhase.RESULT, viewModel.uiState.value.phase)
            }
        }
        assertEquals(3, assessments.evaluations)
        assertEquals(0, users.saveCalls)
    }

    @Test
    fun aValidTaskResetsTheRetryBudgetForTheNextStep() = runTest {
        val viewModel = start(this)
        assessments.outcome = AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.NO_USER_SPEECH)
        repeat(2) {
            viewModel.submitCallResult(session())
            runCurrent()
            viewModel.retryTask()
            runCurrent()
        }
        assessments.outcome = PlacementTestFixtures.getOutcome(AssessmentResult.PASS)
        passAndContinue(viewModel, this)
        assertEquals(0, viewModel.uiState.value.assessment?.technicalRetryCount)
        assessments.outcome = AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.NO_USER_SPEECH)
        viewModel.submitCallResult(session())
        runCurrent()
        assertTrue(viewModel.uiState.value.result is ReassessmentTransition.RetryTask)
        assertEquals(1, viewModel.uiState.value.assessment?.technicalRetryCount)
    }

    @Test
    fun failedSaveRetriesTheSameConclusionWithoutRegrading() = runTest {
        users.saveFailure = IllegalStateException()
        val viewModel = complete(this)
        assertFalse(viewModel.uiState.value.isSaved)
        assertTrue(viewModel.uiState.value.isSaveFailed)
        val result = viewModel.uiState.value.result
        users.saveFailure = null
        users.saveGate = CompletableDeferred()
        viewModel.retrySave()
        viewModel.retrySave()
        runCurrent()
        assertTrue(viewModel.uiState.value.isSaving)
        assertEquals(2, users.saveCalls)
        users.saveGate?.complete(Unit)
        runCurrent()
        assertEquals(result, viewModel.uiState.value.result)
        assertTrue(viewModel.uiState.value.isSaved)
        assertEquals(NOW_MILLIS, users.savedAtMillis)
        assertEquals(3, assessments.evaluations)
    }

    @Test
    fun retryAfterLostAcknowledgementKeepsTheSameTimestampAndAcceptsItsOwnSavedReceipt() = runTest {
        users.saveFailure = IllegalStateException()
        val viewModel = complete(this)
        val result = viewModel.uiState.value.result as ReassessmentTransition.Promoted
        users.setUser(ReassessmentTestFixtures.getUser(result.newLevel, TrainingProgress(0, 1, TrainingStage.UNIT)).copy(
            lastAssessedAt = Date(result.lastAssessedAtMillis), latestAssessmentCriteria = result.latestAssessmentCriteria
        ))
        users.saveFailure = null
        viewModel.retrySave()
        runCurrent()
        assertTrue(viewModel.uiState.value.isSaved)
        assertEquals(2, users.saveCalls)
        assertEquals(result.lastAssessedAtMillis, users.savedAtMillis)
        assertEquals(3, assessments.evaluations)
    }

    @Test
    fun changedProfileCannotBeOverwrittenByAnOldUnsavedConclusion() = runTest {
        users.saveFailure = IllegalStateException()
        val viewModel = complete(this)
        users.setUser(ReassessmentTestFixtures.getUser(SpeakingLevel.B2))
        users.saveFailure = null
        viewModel.retrySave()
        runCurrent()
        assertTrue(viewModel.uiState.value.isSaveFailed)
        assertFalse(viewModel.uiState.value.isSaved)
        assertEquals(1, users.saveCalls)
    }

    @Test
    fun missingDifferentConfirmationScenarioStopsWithoutPromotion() = runTest {
        tasks.tasks = tasks.tasks.filter { it.targetLevel != SpeakingLevel.B1 } + listOf(ReassessmentTestFixtures.getTask(SpeakingLevel.B1))
        val viewModel = start(this)
        passAndContinue(viewModel, this)
        viewModel.submitCallResult(session())
        runCurrent()
        assertEquals(ReassessmentTransition.Stopped(ReassessmentTransition.StopReason.NO_TASK_AVAILABLE), viewModel.uiState.value.result)
        assertEquals(0, users.saveCalls)
    }

    @Test
    fun duplicateCallCompletionCannotGradeOrAdvanceTwice() = runTest {
        val viewModel = start(this)
        assessments.gate = CompletableDeferred()
        viewModel.submitCallResult(session())
        viewModel.submitCallResult(session())
        runCurrent()
        assertEquals(1, assessments.evaluations)
        assessments.gate?.complete(Unit)
        runCurrent()
        assertEquals(ReassessmentStep.NEXT_PROBE, viewModel.uiState.value.assessment?.step)
    }

    @Test
    fun retryIsOfferedAfterAudioCleanupSoTheFirstTapCanStartTheTask() = runTest {
        val viewModel = start(this)
        assessments.outcome = AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.NO_USER_SPEECH)
        assessments.clearGate = CompletableDeferred()
        viewModel.submitCallResult(session())
        runCurrent()
        assertEquals(ReassessmentPhase.GRADING, viewModel.uiState.value.phase)
        assessments.clearGate?.complete(Unit)
        runCurrent()
        assertEquals(ReassessmentPhase.RESULT, viewModel.uiState.value.phase)
        viewModel.retryTask()
        runCurrent()
        assertEquals(ReassessmentPhase.CALLING, viewModel.uiState.value.phase)
    }

    @Test
    fun exitingDuringGradingCancelsTheAttemptAndClearsTemporaryAudio() = runTest {
        val viewModel = start(this)
        assessments.gate = CompletableDeferred()
        viewModel.submitCallResult(session())
        runCurrent()
        viewModel.exit()
        runCurrent()
        assertEquals(ReassessmentPhase.CLOSED, viewModel.uiState.value.phase)
        assertEquals(listOf(AUDIO_ID), assessments.clearedAudioIds)
        assertEquals(0, users.saveCalls)
    }

    @Test
    fun signingOutCancelsGradingAndHidesThePreviousTask() = runTest {
        val viewModel = start(this)
        assessments.gate = CompletableDeferred()
        viewModel.submitCallResult(session())
        runCurrent()
        auth.signedInUserId.value = null
        runCurrent()
        assessments.gate?.complete(Unit)
        runCurrent()
        assertNull(viewModel.uiState.value.assessment)
        assertEquals(listOf(AUDIO_ID), assessments.clearedAudioIds)
        assertEquals(0, users.saveCalls)
    }

    @Test
    fun assessmentExceptionsBecomeRetryableNullInsteadOfStickingInGrading() = runTest {
        val viewModel = start(this)
        assessments.failure = IllegalStateException()
        viewModel.submitCallResult(session())
        runCurrent()
        assertEquals(ReassessmentPhase.RESULT, viewModel.uiState.value.phase)
        assertTrue(viewModel.uiState.value.result is ReassessmentTransition.RetryTask)
        assertEquals(listOf(AUDIO_ID), assessments.clearedAudioIds)
    }

    @Test
    fun scenarioFailureCanRetryTheSameTaskWithoutRestartingTheFlow() = runTest {
        val viewModel = createViewModel()
        runCurrent()
        scenarios.failure = IllegalStateException()
        viewModel.start()
        runCurrent()
        assertEquals(ReassessmentPhase.ERROR, viewModel.uiState.value.phase)
        val task = viewModel.uiState.value.assessment?.currentTask
        scenarios.failure = null
        viewModel.retryLoad()
        runCurrent()
        assertEquals(ReassessmentPhase.CALLING, viewModel.uiState.value.phase)
        assertEquals(task, viewModel.uiState.value.assessment?.currentTask)
    }

    @Test
    fun reenteringAfterExitStartsAtCurrentLevelConfirmation() = runTest {
        val first = start(this)
        passAndContinue(first, this)
        first.exit()
        store.clear()
        runCurrent()
        val second = start(this)
        assertEquals(ReassessmentStep.CURRENT_CONFIRMATION, second.uiState.value.assessment?.step)
        assertEquals(0, users.saveCalls)
    }

    private fun createViewModel() = ReassessmentViewModel(auth, users, tasks, scenarios, assessments, training, clock).also {
        store.put(VIEW_MODEL_KEY, it)
    }

    private fun start(scope: TestScope): ReassessmentViewModel {
        val viewModel = createViewModel()
        scope.runCurrent()
        viewModel.start()
        scope.runCurrent()
        assertEquals(ReassessmentPhase.CALLING, viewModel.uiState.value.phase)
        return viewModel
    }

    private fun passAndContinue(viewModel: ReassessmentViewModel, scope: TestScope) {
        viewModel.submitCallResult(session())
        scope.runCurrent()
        assertEquals(ReassessmentPhase.NEXT_TASK, viewModel.uiState.value.phase)
        viewModel.continueAssessment()
        scope.runCurrent()
        assertEquals(ReassessmentPhase.CALLING, viewModel.uiState.value.phase)
    }

    private fun complete(scope: TestScope): ReassessmentViewModel {
        val viewModel = start(scope)
        repeat(2) { passAndContinue(viewModel, scope) }
        viewModel.submitCallResult(session())
        scope.runCurrent()
        return viewModel
    }

    private fun session() = CallSessionState(
        state = CallState.ENDED, outcome = CallOutcome.COMPLETED, hasUserSpeech = true,
        assessmentAudioId = AUDIO_ID, assessmentAudioDurationMillis = NOW_MILLIS
    )
}
