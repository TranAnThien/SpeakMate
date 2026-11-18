package com.project.speakmate.ui.training

import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.ContentStatus
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingProgress
import com.project.speakmate.domain.model.TrainingStage
import com.project.speakmate.domain.model.User
import com.project.speakmate.domain.repository.HistoryRepository
import com.project.speakmate.test.FakeTrainingRepository
import com.project.speakmate.test.MainDispatcherRule
import com.project.speakmate.ui.training.state.TrainingContentStatus
import com.project.speakmate.ui.training.state.TrainingProgressStatus
import com.project.speakmate.ui.training.state.UnitResultPresentation
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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

    @Test
    fun resolveContentStatusReturnsCorrectStatusPerIndex() = runTest {
        val viewModel = TrainingViewModel(FakeTrainingRepository())
        val progress = TrainingProgress(currentGoalOrder = 1, currentUnitOrder = 1, stage = TrainingStage.UNIT)

        assertEquals(ContentStatus.COMPLETED, viewModel.resolveContentStatus(0, 0, progress))
        assertEquals(ContentStatus.LOCKED, viewModel.resolveContentStatus(2, 0, progress))
        assertEquals(ContentStatus.COMPLETED, viewModel.resolveContentStatus(1, 0, progress))
        assertEquals(ContentStatus.CURRENT, viewModel.resolveContentStatus(1, 1, progress))
        assertEquals(ContentStatus.LOCKED, viewModel.resolveContentStatus(1, 2, progress))
        assertEquals(ContentStatus.LOCKED, viewModel.resolveContentStatus(1, -1, progress))
    }

    @Test
    fun onUnitCallFinishedPassAdvancesProgressAndSaves() = runTest {
        val historyRepository: HistoryRepository = mockk(relaxed = true)
        coEvery { historyRepository.saveAttemptAndProgress(any(), any()) } returns Result.success(Unit)
        val viewModel = TrainingViewModel(FakeTrainingRepository(), historyRepository)

        viewModel.updateUserData(user(TrainingProgress(0, 0, TrainingStage.UNIT)))
        viewModel.loadUnitPreparation("unit-1")
        advanceUntilIdle()

        viewModel.onUnitCallFinished(AssessmentResult.PASS, null, mapOf("action-unit-1" to true))
        advanceUntilIdle()

        val resultState = viewModel.unitResultState.value
        assertNotNull(resultState)
        assertEquals(UnitResultPresentation.PASS, resultState!!.presentation)
        assertEquals(TrainingProgressStatus.COMPLETED, viewModel.uiState.value.goals[0].units[0].status)
        assertEquals(TrainingProgressStatus.CURRENT, viewModel.uiState.value.goals[0].units[1].status)
    }

    @Test
    fun onUnitCallFinishedFailKeepsProgressUnchanged() = runTest {
        val historyRepository: HistoryRepository = mockk(relaxed = true)
        coEvery { historyRepository.saveAttemptAndProgress(any(), any()) } returns Result.success(Unit)
        val viewModel = TrainingViewModel(FakeTrainingRepository(), historyRepository)

        viewModel.updateUserData(user(TrainingProgress(0, 0, TrainingStage.UNIT)))
        viewModel.loadUnitPreparation("unit-1")
        advanceUntilIdle()

        viewModel.onUnitCallFinished(AssessmentResult.FAIL, null, emptyMap())
        advanceUntilIdle()

        val resultState = viewModel.unitResultState.value
        assertNotNull(resultState)
        assertEquals(UnitResultPresentation.FAIL_CRITERIA, resultState!!.presentation)
        assertEquals(TrainingProgressStatus.CURRENT, viewModel.uiState.value.goals[0].units[0].status)
    }

    @Test
    fun onUnitCallFinishedNetworkErrorShowsFailureAndDoesNotAdvance() = runTest {
        val historyRepository: HistoryRepository = mockk(relaxed = true)
        coEvery { historyRepository.saveAttemptAndProgress(any(), any()) } returns Result.failure(RuntimeException("Network error"))
        val viewModel = TrainingViewModel(FakeTrainingRepository(), historyRepository)

        viewModel.updateUserData(user(TrainingProgress(0, 0, TrainingStage.UNIT)))
        viewModel.loadUnitPreparation("unit-1")
        advanceUntilIdle()

        viewModel.onUnitCallFinished(AssessmentResult.PASS, null, mapOf("action-unit-1" to true))
        advanceUntilIdle()

        val resultState = viewModel.unitResultState.value
        assertNotNull(resultState)
        assertEquals(UnitResultPresentation.INSUFFICIENT_EVIDENCE, resultState!!.presentation)
        assertEquals(TrainingProgressStatus.CURRENT, viewModel.uiState.value.goals[0].units[0].status)
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
