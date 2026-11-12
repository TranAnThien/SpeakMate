package com.project.speakmate.ui.practice

import androidx.lifecycle.SavedStateHandle
import com.project.speakmate.test.FakePracticeRepository
import com.project.speakmate.test.MainDispatcherRule
import com.project.speakmate.ui.practice.state.ScenarioCatalogPresentation
import com.project.speakmate.ui.practice.state.ScenarioPreparationPresentation
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PracticeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun catalogLoadsCategoriesAndFiltersScenarios() = runTest {
        val viewModel = PracticeViewModel(FakePracticeRepository())

        assertEquals(ScenarioCatalogPresentation.POPULATED, viewModel.uiState.value.presentation)
        assertEquals(listOf("ALL", "Cafe", "Travel"), viewModel.uiState.value.categories.map { it.id })

        viewModel.onCategorySelected("Travel")

        assertEquals("Travel", viewModel.uiState.value.selectedCategoryId)
        assertEquals(listOf("practice-2"), viewModel.uiState.value.scenarios.map { it.id })
    }

    @Test
    fun preparationLoadsScenarioAndRequiredMetadataBeforeCall() = runTest {
        val viewModel = ScenarioPreparationViewModel(
            SavedStateHandle(mapOf("practiceId" to "practice-1")),
            FakePracticeRepository()
        )

        val state = viewModel.uiState.value

        assertEquals(ScenarioPreparationPresentation.SUCCESS, state.presentation)
        assertEquals("scenario-1", state.scenarioId)
        assertEquals("At a coffee shop", state.context)
        assertEquals("Barista", state.aiRole)
        assertEquals("Order a drink naturally", state.preCallHint)
        assertTrue(state.scenario?.promptTemplate == "Prompt")
    }
}
