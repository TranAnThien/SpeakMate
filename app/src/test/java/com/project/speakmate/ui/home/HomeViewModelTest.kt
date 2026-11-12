package com.project.speakmate.ui.home

import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingProgress
import com.project.speakmate.domain.model.TrainingStage
import com.project.speakmate.domain.model.User
import com.project.speakmate.test.FakeTrainingRepository
import com.project.speakmate.test.MainDispatcherRule
import com.project.speakmate.ui.home.state.HomePresentation
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun unassessedUserShowsAssessmentState() = runTest {
        val viewModel = HomeViewModel(FakeTrainingRepository())

        viewModel.updateUserData(user(level = SpeakingLevel.NOT_ASSESSED, progress = null))

        assertEquals(HomePresentation.UNASSESSED, viewModel.uiState.value.presentation)
    }

    @Test
    fun activeUserShowsCurrentUnitFromProgress() = runTest {
        val viewModel = HomeViewModel(FakeTrainingRepository())

        viewModel.updateUserData(user(progress = TrainingProgress(0, 1, TrainingStage.UNIT)))

        assertEquals(HomePresentation.ACTIVE, viewModel.uiState.value.presentation)
        assertEquals("Goal 1", viewModel.uiState.value.goalTitle)
        assertEquals("Title unit-2", viewModel.uiState.value.unitTitle)
    }

    @Test
    fun completedA1PathShowsNextLevelState() = runTest {
        val viewModel = HomeViewModel(FakeTrainingRepository())

        viewModel.updateUserData(user(progress = TrainingProgress(0, 0, TrainingStage.PATH_COMPLETED)))

        assertEquals(HomePresentation.PATH_COMPLETE, viewModel.uiState.value.presentation)
        assertEquals("A2", viewModel.uiState.value.nextLevel)
    }

    @Test
    fun completedB2PathShowsHighestLevelState() = runTest {
        val viewModel = HomeViewModel(FakeTrainingRepository())

        viewModel.updateUserData(
            user(level = SpeakingLevel.B2, progress = TrainingProgress(0, 0, TrainingStage.PATH_COMPLETED))
        )

        assertEquals(HomePresentation.HIGHEST_LEVEL_COMPLETE, viewModel.uiState.value.presentation)
    }

    private fun user(
        level: SpeakingLevel = SpeakingLevel.A1,
        progress: TrainingProgress? = TrainingProgress(0, 0, TrainingStage.UNIT)
    ) = User(
        userId = "user-1",
        displayName = "Student",
        email = "student@example.com",
        photoUrl = "",
        level = level,
        trainingProgress = progress
    )
}
