package com.project.speakmate.ui.practice

import androidx.lifecycle.SavedStateHandle
import com.project.speakmate.domain.model.FreePractice
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.repository.PracticeRepository
import com.project.speakmate.test.FakePracticeRepository
import com.project.speakmate.test.MainDispatcherRule
import com.project.speakmate.test.SavedStateHandleRule
import com.project.speakmate.ui.practice.state.ScenarioCatalogPresentation
import com.project.speakmate.ui.practice.state.ScenarioPreparationPresentation
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val savedStateHandleRule = SavedStateHandleRule()

    @Test
    fun catalogLoadsCategoriesAndFiltersScenarios() = runTest {
        val viewModel = PracticeViewModel(FakePracticeRepository())

        assertEquals(ScenarioCatalogPresentation.POPULATED, viewModel.uiState.value.presentation)
        assertEquals(listOf("ALL", "Cafe", "Travel"), viewModel.uiState.value.categories.map { it.id })

        viewModel.selectCategory("Travel")

        assertEquals("Travel", viewModel.uiState.value.selectedCategoryId)
        assertEquals(listOf("practice-2"), viewModel.uiState.value.scenarios.map { it.id })
    }

    @Test
    fun preparationLoadsScenarioAndRequiredMetadataBeforeCall() = runTest {
        val viewModel = ScenarioPreparationViewModel(
            SavedStateHandle(mapOf("practiceId" to "practice-1")),
            FakePracticeRepository(),
            com.project.speakmate.test.FakeAuthRepository()
        )

        val state = viewModel.uiState.value

        assertEquals(ScenarioPreparationPresentation.SUCCESS, state.presentation)
        assertEquals("scenario-1", state.scenarioId)
        assertEquals("At a coffee shop", state.context)
        assertEquals("Barista", state.aiRole)
        assertEquals("Order a drink naturally", state.preCallHint)
        assertTrue(state.scenario?.promptTemplate == "Prompt")
    }

    @Test
    fun rapidCategoryChangesOnlyFilterTheMappedSnapshot() = runTest {
        val repository = mockk<PracticeRepository>()
        coEvery { repository.getScenarioCatalog() } returns Result.success(listOf(
            FreePractice("cafe", "cafe", "Cafe"), FreePractice("travel", "travel", "Travel")
        ))
        coEvery { repository.getScenario(any()) } answers { Result.success(Scenario(firstArg(), "Title", "Description", "Prompt")) }
        coEvery { repository.getScenarioPreparationMetadata(any()) } returns Result.failure(IllegalStateException())
        val viewModel = PracticeViewModel(repository)

        viewModel.selectCategory("Cafe")
        viewModel.selectCategory("Travel")
        assertEquals(listOf("travel"), viewModel.uiState.value.scenarios.map { it.id })
        viewModel.selectCategory("ALL")
        runCurrent()

        assertEquals("ALL", viewModel.uiState.value.selectedCategoryId)
        assertEquals(listOf("cafe", "travel"), viewModel.uiState.value.scenarios.map { it.id })
        coVerify(exactly = 2) { repository.getScenario(any()) }
        coVerify(exactly = 2) { repository.getScenarioPreparationMetadata(any()) }
    }

    @Test
    fun cancelledReloadCannotPublishItsLateCatalog() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = mockk<PracticeRepository>()
        var requests = 0
        coEvery { repository.getScenarioCatalog() } coAnswers {
            if (++requests == 1) withContext(NonCancellable) {
                gate.await()
                Result.success(listOf(FreePractice("old", "old", "Old")))
            } else Result.success(listOf(FreePractice("new", "new", "New")))
        }
        coEvery { repository.getScenario(any()) } answers { Result.success(Scenario(firstArg(), "Title", "Description", "Prompt")) }
        coEvery { repository.getScenarioPreparationMetadata(any()) } returns Result.failure(IllegalStateException())
        val viewModel = PracticeViewModel(repository)
        viewModel.loadCatalog()
        gate.complete(Unit)
        runCurrent()

        assertEquals(listOf("new"), viewModel.uiState.value.scenarios.map { it.id })
        assertEquals(listOf("ALL", "New"), viewModel.uiState.value.categories.map { it.id })
        coVerify(exactly = 0) { repository.getScenario("old") }
    }

    @Test
    fun selectingDuringMetadataReloadKeepsTheLatestCategory() = runTest {
        val repository = mockk<PracticeRepository>()
        val gate = CompletableDeferred<Unit>()
        var isReload = false
        coEvery { repository.getScenarioCatalog() } returns Result.success(listOf(
            FreePractice("cafe", "cafe", "Cafe"), FreePractice("travel", "travel", "Travel")
        ))
        coEvery { repository.getScenario(any()) } answers { Result.success(Scenario(firstArg(), "Title", "Description", "Prompt")) }
        coEvery { repository.getScenarioPreparationMetadata(any()) } coAnswers {
            if (isReload) gate.await()
            Result.failure(IllegalStateException())
        }
        val viewModel = PracticeViewModel(repository)
        isReload = true
        viewModel.loadCatalog()
        viewModel.selectCategory("Cafe")
        viewModel.selectCategory("Travel")
        assertEquals(ScenarioCatalogPresentation.POPULATED, viewModel.uiState.value.presentation)
        assertTrue(viewModel.uiState.value.isRefreshing)
        assertEquals("Travel", viewModel.uiState.value.selectedCategoryId)
        assertEquals(listOf("travel"), viewModel.uiState.value.scenarios.map { it.id })
        gate.complete(Unit)
        runCurrent()

        assertEquals("Travel", viewModel.uiState.value.selectedCategoryId)
        assertEquals(listOf("travel"), viewModel.uiState.value.scenarios.map { it.id })
        assertEquals(false, viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun reloadResetsRemovedCategoryAndEmptyCatalogIsUnconfigured() = runTest {
        val repository = mockk<PracticeRepository>()
        coEvery { repository.getScenarioCatalog() } returns Result.success(listOf(FreePractice("cafe", "cafe", "Cafe")))
        coEvery { repository.getScenario(any()) } answers { Result.success(Scenario(firstArg(), "Title", "Description", "Prompt")) }
        coEvery { repository.getScenarioPreparationMetadata(any()) } returns Result.failure(IllegalStateException())
        val viewModel = PracticeViewModel(repository)
        viewModel.selectCategory("Cafe")
        viewModel.selectCategory("missing")
        assertEquals("Cafe", viewModel.uiState.value.selectedCategoryId)
        coEvery { repository.getScenarioCatalog() } returns Result.success(emptyList())
        viewModel.loadCatalog()

        assertEquals("ALL", viewModel.uiState.value.selectedCategoryId)
        assertEquals(ScenarioCatalogPresentation.UNCONFIGURED, viewModel.uiState.value.presentation)
        assertTrue(viewModel.uiState.value.scenarios.isEmpty())
    }

    @Test
    fun repositoryExceptionBecomesLoadErrorAndRetryRecovers() = runTest {
        val repository = mockk<PracticeRepository>()
        coEvery { repository.getScenarioCatalog() } throws IllegalStateException()
        val viewModel = PracticeViewModel(repository)
        assertEquals(ScenarioCatalogPresentation.LOAD_ERROR, viewModel.uiState.value.presentation)
        coEvery { repository.getScenarioCatalog() } returns Result.success(emptyList())
        viewModel.loadCatalog()
        assertEquals(ScenarioCatalogPresentation.UNCONFIGURED, viewModel.uiState.value.presentation)
    }

    @Test
    fun cancellationIsNotPublishedAsALoadFailure() = runTest {
        val repository = mockk<PracticeRepository>()
        coEvery { repository.getScenarioCatalog() } throws CancellationException()
        val viewModel = PracticeViewModel(repository)
        assertEquals(ScenarioCatalogPresentation.LOADING, viewModel.uiState.value.presentation)
        coEvery { repository.getScenarioCatalog() } returns Result.success(emptyList())
        viewModel.loadCatalog()
        assertEquals(ScenarioCatalogPresentation.UNCONFIGURED, viewModel.uiState.value.presentation)
    }
}
