package com.project.speakmate.ui.training

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.project.speakmate.data.session.PracticeCompletionTestFixtures
import com.project.speakmate.domain.model.AssessmentOutcome
import com.project.speakmate.domain.model.InsufficientEvidenceReason
import com.project.speakmate.domain.model.PracticeCompletionState
import com.project.speakmate.domain.model.SpeakingCriteriaResult
import com.project.speakmate.domain.model.TrainingProgress
import com.project.speakmate.domain.model.TrainingStage
import com.project.speakmate.test.FakeAuthRepository
import com.project.speakmate.test.FakeUserRepository
import com.project.speakmate.test.MainDispatcherRule
import com.project.speakmate.test.SavedStateHandleRule
import com.project.speakmate.ui.training.state.GoalAssessmentPresentation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GoalAssessmentResultViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @get:Rule val savedStateHandleRule = SavedStateHandleRule()
    private val store = ViewModelStore()
    private val completion = FakePracticeCompletionRepository()
    private val training = FakeGoalTrainingRepository()
    private val auth = FakeAuthRepository()
    private val users =
        FakeUserRepository(
            GoalAssessmentTestFixtures.user(
                TrainingProgress(currentGoalOrder = 1, currentUnitOrder = 0, stage = TrainingStage.UNIT)
            )
        )

    @After
    fun clearViewModel() {
        store.clear()
    }

    @Test
    fun gradingHasNoSavedOrNextGoalState() = runTest {
        val state = createViewModel().uiState.value
        assertEquals(GoalAssessmentPresentation.PROCESSING, state.presentation)
        assertFalse(state.isSaved)
        assertNull(state.nextGoalId)
    }

    @Test
    fun savedPassOpensTheNextConfiguredGoal() = runTest {
        completion.state.value = PracticeCompletionState.Saved(GoalAssessmentTestFixtures.completion())
        val state = createViewModel().uiState.value
        assertEquals(GoalAssessmentPresentation.NEXT_GOAL, state.presentation)
        assertEquals(training.path.goals[1].goalId, state.nextGoalId)
        assertTrue(state.isSaved)
        assertEquals(PracticeCompletionTestFixtures.FEEDBACK, state.feedback)
    }

    @Test
    fun savedPassWaitsForConfirmedProgressBeforeShowingTheNextGoal() = runTest {
        val viewModel = createViewModel()
        training.pathGate = CompletableDeferred()
        completion.state.value = PracticeCompletionState.Saved(GoalAssessmentTestFixtures.completion())
        runCurrent()
        assertEquals(GoalAssessmentPresentation.PROCESSING, viewModel.uiState.value.presentation)
        assertNull(viewModel.uiState.value.nextGoalId)
        training.pathGate?.complete(Unit)
        runCurrent()
        assertEquals(GoalAssessmentPresentation.NEXT_GOAL, viewModel.uiState.value.presentation)
    }

    @Test
    fun savedLastGoalPassShowsPathCompletionWithoutPromotingTheLevel() = runTest {
        users.setUser(
            GoalAssessmentTestFixtures.user(
                TrainingProgress(currentGoalOrder = 1, currentUnitOrder = 0, stage = TrainingStage.PATH_COMPLETED)
            )
        )
        completion.state.value = PracticeCompletionState.Saved(GoalAssessmentTestFixtures.completion(goalIndex = 1))
        val state = createViewModel(GoalAssessmentTestFixtures.arguments(goalIndex = 1)).uiState.value
        assertEquals(GoalAssessmentPresentation.PATH_COMPLETED, state.presentation)
        assertEquals(0, users.saveCalls)
    }

    @Test
    fun earlierGoalDoesNotClaimToBeTheFinalGoalWhenAnotherSessionCompletedThePath() = runTest {
        users.setUser(
            GoalAssessmentTestFixtures.user(
                TrainingProgress(currentGoalOrder = 1, currentUnitOrder = 0, stage = TrainingStage.PATH_COMPLETED)
            )
        )
        completion.state.value = PracticeCompletionState.Saved(GoalAssessmentTestFixtures.completion())
        val state = createViewModel().uiState.value
        assertEquals(GoalAssessmentPresentation.NEXT_GOAL, state.presentation)
        assertEquals(training.path.goals[1].goalId, state.nextGoalId)
    }

    @Test
    fun replayPassDoesNotPresentAnotherGoalAsNewlyOpened() = runTest {
        users.setUser(
            GoalAssessmentTestFixtures.user(
                TrainingProgress(currentGoalOrder = 1, currentUnitOrder = 0, stage = TrainingStage.PATH_COMPLETED)
            )
        )
        completion.state.value = PracticeCompletionState.Saved(GoalAssessmentTestFixtures.completion())
        val state = createViewModel(GoalAssessmentTestFixtures.arguments(isReplay = true)).uiState.value
        assertEquals(GoalAssessmentPresentation.REPLAY_COMPLETED, state.presentation)
        assertNull(state.nextGoalId)
        assertEquals(0, users.saveCalls)
    }

    @Test
    fun failedSavePreservesPassAndRetriesTheSameAttemptOnlyOnceWhileSaving() = runTest {
        val graded = GoalAssessmentTestFixtures.completion()
        completion.state.value = PracticeCompletionState.SaveFailed(graded)
        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.isSaveFailed)
        assertFalse(viewModel.uiState.value.isSaved)
        assertNull(viewModel.uiState.value.nextGoalId)
        assertEquals(PracticeCompletionTestFixtures.FEEDBACK, viewModel.uiState.value.feedback)
        viewModel.retrySave()
        viewModel.retrySave()
        assertEquals(listOf(GoalAssessmentTestFixtures.ATTEMPT_ID), completion.retriedIds)
        assertTrue(viewModel.uiState.value.isSaving)
        completion.state.value = PracticeCompletionState.Saved(graded)
        runCurrent()
        assertEquals(GoalAssessmentPresentation.NEXT_GOAL, viewModel.uiState.value.presentation)
        assertFalse(viewModel.uiState.value.isSaveFailed)
    }

    @Test
    fun insufficientEvidenceHasNoProgressAction() = runTest {
        val graded =
            GoalAssessmentTestFixtures.completion(
                outcome = AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.EVALUATION_FAILED)
            )
        completion.state.value = PracticeCompletionState.Saved(graded)
        val state = createViewModel().uiState.value
        assertEquals(GoalAssessmentPresentation.INSUFFICIENT_EVIDENCE, state.presentation)
        assertNull(state.nextGoalId)
        assertTrue(state.isSaved)
    }

    @Test
    fun partialAndBothFailureKindsUseTheirOwnResultPresentations() = runTest {
        val cases =
            listOf(
                PracticeCompletionTestFixtures.evaluated(
                    SpeakingCriteriaResult(
                        rangeMet = true,
                        accuracyMet = true,
                        fluencyMet = true,
                        interactionMet = false,
                        coherenceMet = false
                    )
                ) to GoalAssessmentPresentation.PARTIAL,
                PracticeCompletionTestFixtures.evaluated(
                    SpeakingCriteriaResult(
                        rangeMet = true,
                        accuracyMet = true,
                        fluencyMet = false,
                        interactionMet = false,
                        coherenceMet = false
                    )
                ) to GoalAssessmentPresentation.FAIL_CRITERIA,
                PracticeCompletionTestFixtures.evaluated(isActionCompleted = false) to
                    GoalAssessmentPresentation.FAIL_REQUIRED_ACTION
            )
        val viewModel = createViewModel()
        for ((outcome, expected) in cases) {
            completion.state.value =
                PracticeCompletionState.Saved(GoalAssessmentTestFixtures.completion(outcome = outcome))
            runCurrent()
            assertEquals(expected, viewModel.uiState.value.presentation)
            assertNull(viewModel.uiState.value.nextGoalId)
        }
    }

    @Test
    fun missingRuntimeResultReturnsUnavailableAfterProcessDeath() = runTest {
        completion.state.value = PracticeCompletionState.Unavailable
        assertTrue(createViewModel().uiState.value.isUnavailable)
    }

    @Test
    fun retryRecoversProgressLookupWithoutRetryingTheSave() = runTest {
        completion.state.value = PracticeCompletionState.Saved(GoalAssessmentTestFixtures.completion())
        training.failure = IllegalStateException()
        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.hasLoadError)
        training.failure = null
        viewModel.retry()
        runCurrent()
        assertEquals(GoalAssessmentPresentation.NEXT_GOAL, viewModel.uiState.value.presentation)
        assertTrue(completion.retriedIds.isEmpty())
    }

    @Test
    fun leavingTheResultReleasesItsCompletionEntry() = runTest {
        createViewModel()
        store.clear()
        assertEquals(listOf(GoalAssessmentTestFixtures.ATTEMPT_ID), completion.releasedIds)
    }

    @Test
    fun signingOutHidesThePreviousUsersResult() = runTest {
        completion.state.value = PracticeCompletionState.Saved(GoalAssessmentTestFixtures.completion())
        val viewModel = createViewModel()
        auth.signedInUserId.value = null
        runCurrent()
        assertTrue(viewModel.uiState.value.isUnavailable)
    }

    private fun createViewModel(arguments: SavedStateHandle = GoalAssessmentTestFixtures.arguments()) =
        GoalAssessmentResultViewModel(
            savedStateHandle = arguments,
            completionRepository = completion,
            trainingRepository = training,
            authRepository = auth,
            userRepository = users
        )
            .also { store.put(GoalAssessmentTestFixtures.VIEW_MODEL_KEY, it) }
}
