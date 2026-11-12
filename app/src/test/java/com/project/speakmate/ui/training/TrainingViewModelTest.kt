package com.project.speakmate.ui.training

import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingProgress
import com.project.speakmate.domain.model.TrainingStage
import com.project.speakmate.domain.model.User
import com.project.speakmate.test.FakeTrainingRepository
import com.project.speakmate.test.MainDispatcherRule
import com.project.speakmate.ui.training.state.TrainingContentStatus
import com.project.speakmate.ui.training.state.TrainingProgressStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TrainingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun pathUsesConfiguredGoalAndUnitOrderForProgress() = runTest {
        val viewModel = TrainingViewModel(FakeTrainingRepository())

        viewModel.updateUserData(user(TrainingProgress(0, 1, TrainingStage.UNIT)))

        val state = viewModel.uiState.value
        assertEquals(TrainingContentStatus.CONTENT, state.status)
        assertEquals(TrainingProgressStatus.COMPLETED, state.goals[0].units[0].status)
        assertEquals(TrainingProgressStatus.CURRENT, state.goals[0].units[1].status)
        assertEquals(TrainingProgressStatus.LOCKED, state.goals[1].status)
    }

    @Test
    fun invalidProgressShowsConfigurationError() = runTest {
        val viewModel = TrainingViewModel(FakeTrainingRepository())

        viewModel.updateUserData(user(TrainingProgress(0, 9, TrainingStage.UNIT)))

        assertEquals(TrainingContentStatus.ERROR, viewModel.uiState.value.status)
        assertEquals(true, viewModel.uiState.value.errorMessage.contains("currentUnitOrder"))
    }

    @Test
    fun preparationCombinesScenarioAndUnitContent() = runTest {
        val viewModel = TrainingViewModel(FakeTrainingRepository())

        viewModel.loadUnitPreparation("unit-1")

        val state = viewModel.preparationState.value
        assertEquals(TrainingContentStatus.CONTENT, state.status)
        assertEquals("Bối cảnh: Scenario context", state.description)
        assertEquals("Focus unit-1", state.overview.communicativeFocus)
        assertEquals(listOf("Action unit-1"), state.overview.requiredActions)
        assertEquals("https://example.com/image.jpg", state.overview.imageUrl)
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
