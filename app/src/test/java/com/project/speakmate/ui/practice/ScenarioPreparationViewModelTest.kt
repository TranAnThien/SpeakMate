package com.project.speakmate.ui.practice

import androidx.lifecycle.SavedStateHandle
import com.project.speakmate.test.SavedStateHandleRule
import com.project.speakmate.ui.practice.state.ScenarioPreparationPresentation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScenarioPreparationViewModelTest {
    @get:Rule
    val savedStateHandleRule = SavedStateHandleRule()

    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun selectedFreePracticeLoadsReferencedScenarioBeforeCallCanStart() = runTest {
        val viewModel = ScenarioPreparationViewModel(
            SavedStateHandle(mapOf("practiceId" to "fp-1")),
            FakeScenarioPreparationPracticeRepository()
        )

        val state = viewModel.uiState.value

        assertEquals(ScenarioPreparationPresentation.SUCCESS, state.presentation)
        assertEquals("FP-SC", state.scenarioId)
        assertEquals("Coffee chat", state.title)
        assertEquals("https://example.com/coffee.jpg", state.imageUrl)
        assertEquals("You are in a coffee shop", state.context)
        assertEquals("Barista", state.aiRole)
        assertEquals("Ask about the menu", state.preCallHint)

        assertEquals("Prompt", state.scenario?.promptTemplate)
        viewModel.prepareCall()
        assertTrue(viewModel.uiState.value.isInitializing)

        viewModel.resetCallPreparation()
        assertTrue(!viewModel.uiState.value.isInitializing)
    }
}
