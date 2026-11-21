package com.project.speakmate.ui.training

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.project.speakmate.domain.model.TrainingActivity
import com.project.speakmate.domain.model.TrainingProgress
import com.project.speakmate.domain.model.TrainingStage
import com.project.speakmate.test.FakeAuthRepository
import com.project.speakmate.test.FakeUserRepository
import com.project.speakmate.test.MainDispatcherRule
import com.project.speakmate.test.SavedStateHandleRule
import com.project.speakmate.ui.training.state.TrainingContentStatus
import com.project.speakmate.utils.AppConstant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GoalAssessmentViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val savedStateHandleRule = SavedStateHandleRule()
    private val store = ViewModelStore()
    private val training = FakeGoalTrainingRepository()
    private val auth = FakeAuthRepository()
    private val users = FakeUserRepository(GoalAssessmentTestFixtures.user())

    @After
    fun clearViewModel() {
        store.clear()
    }

    @Test
    fun currentGoalLoadsItsOwnScenarioActionsAndPathLevel() = runTest {
        val state = createViewModel().uiState.value
        val goal = training.path.goals[0]
        val activity = checkNotNull(state.activity)
        assertEquals(TrainingContentStatus.CONTENT, state.status)
        assertEquals(listOf(goal.goalAssessment.scenarioId), training.requestedScenarioIds)
        assertEquals(goal.goalAssessment.requiredActions, activity.requiredActions)
        assertEquals(goal.canDoDescription, activity.communicativeGoal)
        assertEquals(training.path.targetLevel, activity.targetLevel)
        assertEquals(TrainingActivity.GoalAssessmentActivity(0), activity.activity)
        assertFalse(state.isReplay)
    }

    @Test
    fun goalAssessmentRemainsLockedBeforeTheLastUnitPasses() = runTest {
        users.setUser(GoalAssessmentTestFixtures.user(TrainingProgress(0, 0, TrainingStage.UNIT)))
        val state = createViewModel().uiState.value
        assertTrue(state.isLocked)
        assertNull(state.activity)
        assertTrue(training.requestedScenarioIds.isEmpty())
    }

    @Test
    fun laterGoalAssessmentRemainsLocked() = runTest {
        val state = createViewModel(GoalAssessmentTestFixtures.arguments(1)).uiState.value
        assertTrue(state.isLocked)
        assertTrue(training.requestedScenarioIds.isEmpty())
    }

    @Test
    fun previousGoalReplayKeepsItsConfiguredActionsAndFallsBackWithoutVariants() = runTest {
        users.setUser(GoalAssessmentTestFixtures.user(TrainingProgress(1, 0, TrainingStage.UNIT)))
        val viewModel = createViewModel()
        val original = viewModel.uiState.value.activity
        assertTrue(viewModel.uiState.value.isReplay)
        viewModel.prepareRetry()
        assertEquals(original, viewModel.uiState.value.activity)
        assertEquals(0, users.saveCalls)
    }

    @Test
    fun completedPathStillAllowsLastGoalReplay() = runTest {
        users.setUser(GoalAssessmentTestFixtures.user(TrainingProgress(1, 0, TrainingStage.PATH_COMPLETED)))
        val state = createViewModel(GoalAssessmentTestFixtures.arguments(1)).uiState.value
        assertTrue(state.isReplay)
        assertNotNull(state.activity)
    }

    @Test
    fun missingGoalArgumentCannotStartACall() = runTest {
        val state = createViewModel(SavedStateHandle()).uiState.value
        assertTrue(state.isLocked)
        assertNull(state.activity)
    }

    @Test
    fun retryReplacesAFailedProfileListener() = runTest {
        users.shouldFailObservation = true
        val viewModel = createViewModel()
        assertEquals(TrainingContentStatus.ERROR, viewModel.uiState.value.status)
        users.shouldFailObservation = false
        viewModel.retry()
        runCurrent()
        assertEquals(TrainingContentStatus.CONTENT, viewModel.uiState.value.status)
        assertEquals(1, users.activeObservers)
    }

    @Test
    fun signingOutDropsThePreparedActivity() = runTest {
        val viewModel = createViewModel()
        auth.signedInUserId.value = null
        runCurrent()
        assertNull(viewModel.uiState.value.activity)
        assertEquals(0, users.activeObservers)
    }

    @Test
    fun retryRecoversAfterTrainingContentFailsToLoad() = runTest {
        training.failure = IllegalStateException()
        val viewModel = createViewModel()
        assertEquals(TrainingContentStatus.ERROR, viewModel.uiState.value.status)
        training.failure = null
        viewModel.retry()
        runCurrent()
        assertEquals(TrainingContentStatus.CONTENT, viewModel.uiState.value.status)
    }

    private fun createViewModel(arguments: SavedStateHandle = GoalAssessmentTestFixtures.arguments()) =
        GoalAssessmentViewModel(arguments, training, auth, users).also { store.put(GoalAssessmentTestFixtures.VIEW_MODEL_KEY, it) }
}
