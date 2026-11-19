package com.project.speakmate.ui.training

import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.ProgressProblem
import com.project.speakmate.domain.model.TrainingProgress
import com.project.speakmate.domain.model.TrainingStage
import com.project.speakmate.domain.model.User
import com.project.speakmate.test.FakeAuthRepository
import com.project.speakmate.test.FakeUserRepository
import com.project.speakmate.test.FakeTrainingRepository
import com.project.speakmate.test.MainDispatcherRule
import com.project.speakmate.ui.training.state.TrainingContentStatus
import com.project.speakmate.ui.training.state.TrainingProgressStatus
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrainingPathViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun pathUsesConfiguredGoalAndUnitOrderForProgress() = runTest {
        val viewModel = createViewModel(user(TrainingProgress(0, 1, TrainingStage.UNIT)))

        val state = viewModel.uiState.value
        assertEquals(TrainingContentStatus.CONTENT, state.status)
        assertEquals(TrainingProgressStatus.COMPLETED, state.goals[0].units[0].status)
        assertEquals(TrainingProgressStatus.CURRENT, state.goals[0].units[1].status)
        assertEquals(TrainingProgressStatus.LOCKED, state.goals[1].status)
    }

    @Test
    fun invalidProgressShowsConfigurationError() = runTest {
        val viewModel = createViewModel(user(TrainingProgress(0, 9, TrainingStage.UNIT)))

        assertEquals(TrainingContentStatus.ERROR, viewModel.uiState.value.status)
        assertEquals(ProgressProblem.UNIT_OUT_OF_RANGE, viewModel.uiState.value.progressProblem)
    }

    private fun createViewModel(user: User): TrainingPathViewModel =
        TrainingPathViewModel(FakeTrainingRepository(), FakeAuthRepository(user.userId), FakeUserRepository(user))

    @Test
    fun profileUpdatesChangeTheCurrentUnitWithoutNavigationReload() = runTest {
        val users = FakeUserRepository(user(TrainingProgress(0, 0, TrainingStage.UNIT)))
        val viewModel = TrainingPathViewModel(FakeTrainingRepository(), FakeAuthRepository(), users)

        users.setUser(user(TrainingProgress(0, 1, TrainingStage.UNIT)))
        advanceUntilIdle()

        assertEquals(TrainingProgressStatus.CURRENT, viewModel.uiState.value.goals[0].units[1].status)
        assertEquals(1, users.activeObservers)
    }

    @Test
    fun retryReplacesTheFailedProfileListener() = runTest {
        val users = FakeUserRepository(user(TrainingProgress(0, 0, TrainingStage.UNIT)))
        users.shouldFailObservation = true
        val viewModel = TrainingPathViewModel(FakeTrainingRepository(), FakeAuthRepository(), users)
        assertEquals(TrainingContentStatus.ERROR, viewModel.uiState.value.status)

        users.shouldFailObservation = false
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(TrainingContentStatus.CONTENT, viewModel.uiState.value.status)
        assertEquals(2, users.observationStarts)
        assertEquals(1, users.activeObservers)
    }

    @Test
    fun completedPathHasNoLockedUnitsOrCurrentGoal() = runTest {
        val viewModel = createViewModel(user(TrainingProgress(1, 0, TrainingStage.PATH_COMPLETED)))
        val state = viewModel.uiState.value

        assertEquals(false, state.canContinue)
        assertEquals(true, state.goals.all { it.status == TrainingProgressStatus.COMPLETED })
        assertEquals(true, state.goals.flatMap { it.units }.all { it.status == TrainingProgressStatus.COMPLETED && it.canOpen })
    }

    @Test
    fun currentAssessmentHasAnExplicitGoalDestination() = runTest {
        val state = createViewModel(user(TrainingProgress(0, 1, TrainingStage.GOAL_ASSESSMENT))).uiState.value
        val current = state.goals.flatMap { it.units }.single { it.status == TrainingProgressStatus.CURRENT }
        assertEquals(com.project.speakmate.ui.training.state.TrainingActivityDestination.GoalAssessment("goal-1"), current.destination)
        assertEquals(true, current.canOpen)
    }

    @Test
    fun unitsKeepTheirExplicitUnitDestinations() = runTest {
        val state = createViewModel(user(TrainingProgress(0, 0, TrainingStage.UNIT))).uiState.value
        val current = state.goals.flatMap { it.units }.single { it.status == TrainingProgressStatus.CURRENT }
        assertEquals(com.project.speakmate.ui.training.state.TrainingActivityDestination.Unit("unit-1"), current.destination)
    }

    private fun user(progress: TrainingProgress) = User(
        userId = "user-1",
        displayName = "Student",
        email = "student@example.com",
        photoUrl = "",
        level = SpeakingLevel.A1,
        trainingProgress = progress
    )
}

