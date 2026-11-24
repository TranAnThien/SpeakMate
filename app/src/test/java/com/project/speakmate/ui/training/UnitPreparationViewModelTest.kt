package com.project.speakmate.ui.training

import androidx.lifecycle.ViewModelStore
import com.project.speakmate.domain.model.GrammarPoint
import com.project.speakmate.domain.model.LearningExample
import com.project.speakmate.domain.model.SentencePattern
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingProgress
import com.project.speakmate.domain.model.TrainingStage
import com.project.speakmate.domain.model.User
import com.project.speakmate.test.FakeAuthRepository
import com.project.speakmate.test.FakePronunciationRepository
import com.project.speakmate.test.FakeTrainingRepository
import com.project.speakmate.test.FakeUserRepository
import com.project.speakmate.test.MainDispatcherRule
import com.project.speakmate.test.TrainingTestFixtures
import com.project.speakmate.ui.training.state.PreparationSection
import com.project.speakmate.ui.training.state.TrainingContentStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class UnitPreparationViewModelTest {
    companion object {
        private const val UNIT_ID = "unit-1"
        private const val LOCKED_UNIT_ID = "unit-3"
        private const val VIEW_MODEL_KEY = "preparation"
    }

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val user =
        User(
            userId = "user-1",
            displayName = "Student",
            email = "student@example.com",
            photoUrl = "",
            level = SpeakingLevel.A1,
            trainingProgress = TrainingProgress(currentGoalOrder = 0, currentUnitOrder = 0, stage = TrainingStage.UNIT)
        )
    private val pronunciation = FakePronunciationRepository()

    @Test
    fun preparationCombinesScenarioAndUnitContent() = runTest {
        val viewModel = createViewModel()
        viewModel.getPreparation(UNIT_ID)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TrainingContentStatus.CONTENT, state.status)
        assertEquals("Bối cảnh: Scenario context", state.description)
        assertEquals("Focus unit-1", state.overview.communicativeFocus)
        assertEquals(listOf("Action unit-1"), state.overview.requiredActions)
        assertEquals("https://example.com/image.jpg", state.overview.imageUrl)
        assertEquals(SpeakingLevel.A1, state.activity?.targetLevel)
    }

    @Test
    fun aLockedUnitCannotProvideACallContext() = runTest {
        val viewModel = createViewModel()
        viewModel.getPreparation(LOCKED_UNIT_ID)
        advanceUntilIdle()

        assertEquals(TrainingContentStatus.ERROR, viewModel.uiState.value.status)
        assertEquals(true, viewModel.uiState.value.isLocked)
        assertFalse(viewModel.uiState.value.canStartRoleplay)
        assertNull(viewModel.uiState.value.activity)
    }

    @Test
    fun pronunciationUsesTheRepositoryAndIsReleasedWithTheViewModel() = runTest {
        val viewModel = createViewModel()
        viewModel.getPreparation(UNIT_ID)
        advanceUntilIdle()
        val stopsBeforePlayback = pronunciation.stops
        viewModel.playText("A coffee please")
        viewModel.stopPronunciation()
        assertTrue(pronunciation.stops > stopsBeforePlayback)
        val store = ViewModelStore()
        store.put(VIEW_MODEL_KEY, viewModel)
        store.clear()

        assertEquals(listOf("A coffee please"), pronunciation.playedTexts)
        assertEquals(1, pronunciation.releases)
    }

    @Test
    fun preparationKeepsEveryGrammarAndSentencePatternExample() = runTest {
        val path = TrainingTestFixtures.getPath()
        val goal = path.goals.first()
        val unit = goal.units.first()
        val examples =
            listOf(
                LearningExample("First sentence", "First translation"),
                LearningExample("Second sentence", "Second translation")
            )
        val preparation =
            unit.preparation.copy(
                grammarPoints =
                    listOf(
                        GrammarPoint(
                            title = "Grammar",
                            structure = "Structure",
                            explanation = "Explanation",
                            usage = "Usage",
                            examples = examples
                        )
                    ),
                sentencePatterns =
                    listOf(
                        SentencePattern(pattern = "Pattern", mean = "Meaning", usage = "Usage", examples = examples)
                    )
            )
        val repository =
            FakeTrainingRepository(
                pathResult =
                    Result.success(
                        path.copy(goals = listOf(goal.copy(units = listOf(unit.copy(preparation = preparation)))))
                    )
            )
        val viewModel =
            UnitPreparationViewModel(
                trainingRepository = repository,
                authRepository = FakeAuthRepository(),
                userRepository = FakeUserRepository(user),
                pronunciationRepository = pronunciation
            )
        viewModel.getPreparation(UNIT_ID)
        advanceUntilIdle()

        assertEquals(
            examples.map { it.sentence },
            viewModel.uiState.value.grammar.single().examples.map { it.sentence }
        )
        assertEquals(
            examples.map { it.sentence },
            viewModel.uiState.value.sentencePatterns.single().examples.map { it.sentence }
        )
    }

    @Test
    fun userUpdateWhileInContentKeepsContentSectionAndStartReadiness() = runTest {
        val users = FakeUserRepository(user)
        val viewModel =
            UnitPreparationViewModel(
                trainingRepository = FakeTrainingRepository(),
                authRepository = FakeAuthRepository(),
                userRepository = users,
                pronunciationRepository = pronunciation
            )
        viewModel.getPreparation(UNIT_ID)
        advanceUntilIdle()
        viewModel.selectSection(PreparationSection.GRAMMAR)
        viewModel.prepareStart()
        assertTrue(viewModel.uiState.value.isStarting)
        assertTrue(viewModel.uiState.value.isStartReady)

        users.setUser(user.copy(displayName = "Updated Student"))
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(TrainingContentStatus.CONTENT, state.status)
        assertEquals(PreparationSection.GRAMMAR, state.section)
        assertTrue(state.isStarting)
        assertTrue(state.isStartReady)
    }

    @Test
    fun prepareCallLocksActiveCallAndStopsObservingUntilPreparationResumes() = runTest {
        val users = FakeUserRepository(user)
        val viewModel =
            UnitPreparationViewModel(
                trainingRepository = FakeTrainingRepository(),
                authRepository = FakeAuthRepository(),
                userRepository = users,
                pronunciationRepository = pronunciation
            )
        viewModel.getPreparation(UNIT_ID)
        advanceUntilIdle()
        assertEquals(1, users.activeObservers)

        viewModel.prepareCall(unitId = UNIT_ID, isRetry = false)
        runCurrent()

        assertNotNull(viewModel.uiState.value.activeCallActivity)
        assertFalse(viewModel.uiState.value.activeCallIsReplay)
        assertEquals(0, users.activeObservers)

        viewModel.getPreparation(UNIT_ID)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.activeCallActivity)
        assertEquals(1, users.activeObservers)
    }

    @Test
    fun prepareCallWithRetryLoadsAndLocksWithoutManualRouteState() = runTest {
        val users = FakeUserRepository(user)
        val viewModel =
            UnitPreparationViewModel(
                trainingRepository = FakeTrainingRepository(),
                authRepository = FakeAuthRepository(),
                userRepository = users,
                pronunciationRepository = pronunciation
            )
        viewModel.prepareCall(unitId = UNIT_ID, isRetry = true)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.activeCallActivity)
        assertEquals(0, users.activeObservers)
    }

    private fun createViewModel() =
        UnitPreparationViewModel(
            trainingRepository = FakeTrainingRepository(),
            authRepository = FakeAuthRepository(),
            userRepository = FakeUserRepository(user),
            pronunciationRepository = pronunciation
        )
}
