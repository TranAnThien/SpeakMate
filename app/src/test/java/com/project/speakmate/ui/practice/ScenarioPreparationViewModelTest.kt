package com.project.speakmate.ui.practice

import androidx.lifecycle.SavedStateHandle
import com.project.speakmate.domain.model.FreePractice
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.repository.PracticeRepository
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
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScenarioPreparationViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun selectedFreePractice_loadsReferencedScenarioBeforeCallCanStart() = runTest {
        val viewModel = ScenarioPreparationViewModel(
            SavedStateHandle(mapOf("practiceId" to "fp-1")),
            ScenarioPrepPracticeRepository()
        )

        val state = viewModel.uiState.value

        assertEquals(ScenarioPreparationPresentation.SUCCESS, state.presentation)
        assertEquals("FP-SC", state.scenarioId)
        assertEquals("Coffee chat", state.title)
        assertEquals("https://example.com/coffee.jpg", state.imageUrl)
        assertEquals("You are in a coffee shop", state.context)
        assertEquals("Barista", state.aiRole)
        assertEquals("Ask about the menu", state.preCallHint)
        // The system prompt remains available only to the call layer through Scenario.
        assertEquals("Prompt", state.scenario?.promptTemplate)
        viewModel.onStartCallPressed()
        assertTrue(viewModel.uiState.value.isInitializing)

        viewModel.onCallExited()
        assertTrue(!viewModel.uiState.value.isInitializing)
    }
}

private class ScenarioPrepPracticeRepository : PracticeRepository {
    private val scenario = Scenario("FP-SC", "Coffee chat", "Order a drink", "Prompt")
    override suspend fun getScenarioCatalog(): Result<List<FreePractice>> =
        Result.success(listOf(FreePractice("fp-1", scenario.scenarioId, "Daily")))
    override suspend fun getScenario(scenarioId: String): Result<Scenario> = Result.success(scenario)
    override suspend fun getScenarioPreparationMetadata(scenarioId: String): Result<Map<String, String>> = Result.success(
        mapOf(
            "imageUrl" to "https://example.com/coffee.jpg",
            "uiContext" to "You are in a coffee shop",
            "aiRole" to "Barista",
            "preCallHint" to "Ask about the menu"
        )
    )
}
