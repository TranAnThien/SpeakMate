package com.project.speakmate.ui.training

import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingProgress
import com.project.speakmate.domain.model.TrainingStage
import com.project.speakmate.domain.model.User
import com.project.speakmate.test.FakeAuthRepository
import com.project.speakmate.test.FakeTrainingRepository
import com.project.speakmate.test.FakeUserRepository
import com.project.speakmate.test.MainDispatcherRule
import com.project.speakmate.ui.training.state.TrainingActivityDestination.GoalAssessment
import com.project.speakmate.ui.training.state.TrainingActivityDestination.Unit
import com.project.speakmate.ui.training.state.TrainingContentStatus
import com.project.speakmate.ui.training.state.TrainingPathDialog
import com.project.speakmate.ui.training.state.TrainingPathFailure
import com.project.speakmate.ui.training.state.TrainingPathNotice
import com.project.speakmate.ui.training.state.TrainingProgressStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrainingPathViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun pathUsesConfiguredGoalAndUnitOrderForProgress() = runTest {
        val viewModel =
            createViewModel(
                user(TrainingProgress(currentGoalOrder = 0, currentUnitOrder = 1, stage = TrainingStage.UNIT))
            )

        val state = viewModel.uiState.value
        assertEquals(TrainingContentStatus.CONTENT, state.status)
        assertEquals(TrainingProgressStatus.COMPLETED, state.goals[0].units[0].status)
        assertEquals(TrainingProgressStatus.CURRENT, state.goals[0].units[1].status)
        assertEquals(TrainingProgressStatus.LOCKED, state.goals[1].status)
    }

    @Test
    fun invalidProgressShowsConfigurationError() = runTest {
        val viewModel =
            createViewModel(
                user(TrainingProgress(currentGoalOrder = 0, currentUnitOrder = 9, stage = TrainingStage.UNIT))
            )

        assertEquals(TrainingContentStatus.ERROR, viewModel.uiState.value.status)
        assertEquals(TrainingPathFailure.PROGRESS, viewModel.uiState.value.failure)
    }

    private fun createViewModel(user: User): TrainingPathViewModel =
        TrainingPathViewModel(
            trainingRepository = FakeTrainingRepository(),
            authRepository = FakeAuthRepository(user.userId),
            userRepository = FakeUserRepository(user)
        )

    @Test
    fun profileUpdatesChangeTheCurrentUnitWithoutNavigationReload() = runTest {
        val users =
            FakeUserRepository(
                user(TrainingProgress(currentGoalOrder = 0, currentUnitOrder = 0, stage = TrainingStage.UNIT))
            )
        val viewModel =
            TrainingPathViewModel(
                trainingRepository = FakeTrainingRepository(),
                authRepository = FakeAuthRepository(),
                userRepository = users
            )

        users.setUser(user(TrainingProgress(currentGoalOrder = 0, currentUnitOrder = 1, stage = TrainingStage.UNIT)))
        advanceUntilIdle()

        assertEquals(TrainingProgressStatus.CURRENT, viewModel.uiState.value.goals[0].units[1].status)
        assertEquals(1, users.activeObservers)
    }

    @Test
    fun retryReplacesTheFailedProfileListener() = runTest {
        val users =
            FakeUserRepository(
                user(TrainingProgress(currentGoalOrder = 0, currentUnitOrder = 0, stage = TrainingStage.UNIT))
            )
        users.shouldFailObservation = true
        val viewModel =
            TrainingPathViewModel(
                trainingRepository = FakeTrainingRepository(),
                authRepository = FakeAuthRepository(),
                userRepository = users
            )
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
        val viewModel =
            createViewModel(
                user(
                    TrainingProgress(currentGoalOrder = 1, currentUnitOrder = 0, stage = TrainingStage.PATH_COMPLETED)
                )
            )
        val state = viewModel.uiState.value

        assertEquals(false, state.canContinue)
        assertEquals(true, state.goals.all { it.status == TrainingProgressStatus.COMPLETED })
        assertEquals(
            true,
            state.goals.flatMap { it.units }.all { it.status == TrainingProgressStatus.COMPLETED && it.canOpen }
        )
    }

    @Test
    fun currentAssessmentHasAnExplicitGoalDestination() = runTest {
        val state =
            createViewModel(
                user(
                        TrainingProgress(
                            currentGoalOrder = 0,
                            currentUnitOrder = 1,
                            stage = TrainingStage.GOAL_ASSESSMENT
                        )
                    )
            )
                .uiState
                .value
        val current = state.goals.flatMap { it.units }.single { it.status == TrainingProgressStatus.CURRENT }
        assertEquals(GoalAssessment("goal-1"), current.destination)
        assertEquals(true, current.canOpen)
    }

    @Test
    fun unitsKeepTheirExplicitUnitDestinations() = runTest {
        val state =
            createViewModel(
                user(TrainingProgress(currentGoalOrder = 0, currentUnitOrder = 0, stage = TrainingStage.UNIT))
            )
                .uiState
                .value
        val current = state.goals.flatMap { it.units }.single { it.status == TrainingProgressStatus.CURRENT }
        assertEquals(Unit("unit-1"), current.destination)
    }

    @Test
    fun openActivitySetsPendingNavigationAndConsumeClearsItOnce() = runTest {
        val viewModel =
            createViewModel(
                user(TrainingProgress(currentGoalOrder = 0, currentUnitOrder = 0, stage = TrainingStage.UNIT))
            )
        advanceUntilIdle()

        viewModel.openActivity(Unit("unit-1"))
        advanceUntilIdle()

        assertEquals(Unit("unit-1"), viewModel.uiState.value.pendingNavigation)
        assertEquals(Unit("unit-1"), viewModel.consumePendingNavigation())
        assertNull(viewModel.uiState.value.pendingNavigation)
        assertNull(viewModel.consumePendingNavigation())
    }

    @Test
    fun pendingNavigationIsClearedWhenRefreshFails() = runTest {
        val users =
            FakeUserRepository(
                user(TrainingProgress(currentGoalOrder = 0, currentUnitOrder = 0, stage = TrainingStage.UNIT))
            )
        val viewModel =
            TrainingPathViewModel(
                trainingRepository = FakeTrainingRepository(),
                authRepository = FakeAuthRepository(),
                userRepository = users
            )
        advanceUntilIdle()
        viewModel.openActivity(Unit("unit-1"))
        advanceUntilIdle()
        assertEquals(Unit("unit-1"), viewModel.uiState.value.pendingNavigation)

        users.setUser(user(TrainingProgress(currentGoalOrder = 0, currentUnitOrder = 9, stage = TrainingStage.UNIT)))
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.pendingNavigation)
    }

    @Test
    fun lockedUnitNoticeCoexistsWithOpenGoalSheet() = runTest {
        val viewModel =
            TrainingPathViewModel(
                trainingRepository = FakeTrainingRepository(),
                authRepository = FakeAuthRepository(),
                userRepository =
                    FakeUserRepository(
                        user(TrainingProgress(currentGoalOrder = 0, currentUnitOrder = 0, stage = TrainingStage.UNIT))
                    )
            )
        advanceUntilIdle()
        val goalId = viewModel.uiState.value.goals.first().id
        val lockedDestination =
            viewModel.uiState.value.goals
                .flatMap { it.units }
                .first { it.status == TrainingProgressStatus.LOCKED }
                .destination

        viewModel.showGoal(goalId)
        viewModel.openActivity(lockedDestination)

        assertEquals(TrainingPathDialog.Goal(goalId), viewModel.uiState.value.activeDialog)
        assertEquals(TrainingPathNotice.LOCKED, viewModel.uiState.value.notice)

        viewModel.dismissNotice()
        assertEquals(TrainingPathDialog.Goal(goalId), viewModel.uiState.value.activeDialog)
        assertNull(viewModel.uiState.value.noticeDialog)
    }

    private fun user(progress: TrainingProgress) =
        User(
            userId = "user-1",
            displayName = "Student",
            email = "student@example.com",
            photoUrl = "",
            level = SpeakingLevel.A1,
            trainingProgress = progress
        )
}
