package com.project.speakmate.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.project.speakmate.domain.model.AppLanguage
import com.project.speakmate.domain.repository.AppLanguageRepository
import com.project.speakmate.domain.repository.PracticeCompletionRepository
import com.project.speakmate.test.FakeAuthRepository
import com.project.speakmate.test.MainDispatcherRule
import com.project.speakmate.ui.profile.state.SettingsEffect
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    companion object {
        private const val VIEW_MODEL_KEY = "settings"
    }

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())
    private val store = ViewModelStore()
    private val completions = mockk<PracticeCompletionRepository>()
    private val languages = mockk<AppLanguageRepository>()

    @After
    fun clear() { store.clear() }

    @Test
    fun retryRestartsPendingObserverAfterFailure() = runTest {
        var subscriptions = 0
        every { completions.observePendingCompletions() } answers {
            flow {
                subscriptions++
                if (subscriptions == 1) throw IllegalStateException()
                emit(emptyList())
            }
        }
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.retry()
        advanceUntilIdle()
        viewModel.requestSignOut()
        assertEquals(2, subscriptions)
        assertEquals(0, viewModel.uiState.value.pendingCompletionCount)
    }

    @Test
    fun successfulLanguageSaveRequestsActivityRecreation() = runTest {
        every { completions.observePendingCompletions() } returns flowOf(emptyList())
        coEvery { languages.saveLanguage(AppLanguage.ENGLISH) } returns Result.success(Unit)
        val viewModel = createViewModel()
        val effects = mutableListOf<SettingsEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEffect.collect { effects.add(it) } }
        advanceUntilIdle()
        viewModel.showLanguageNotice()
        viewModel.changeLanguage(AppLanguage.ENGLISH)
        advanceUntilIdle()
        assertEquals(listOf(SettingsEffect.RecreateActivity), effects)
        assertEquals(AppLanguage.ENGLISH, viewModel.uiState.value.selectedLanguage)
    }

    @Test
    fun failedLanguageSaveKeepsTheLanguageAndDoesNotRecreate() = runTest {
        every { completions.observePendingCompletions() } returns flowOf(emptyList())
        coEvery { languages.saveLanguage(AppLanguage.ENGLISH) } returns Result.failure(IllegalStateException())
        val viewModel = createViewModel()
        val effects = mutableListOf<SettingsEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEffect.collect { effects.add(it) } }
        advanceUntilIdle()
        viewModel.showLanguageNotice()
        viewModel.changeLanguage(AppLanguage.ENGLISH)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isLanguageSaveFailed)
        assertEquals(AppLanguage.VIETNAMESE, viewModel.uiState.value.selectedLanguage)
        assertTrue(effects.isEmpty())
    }

    private fun createViewModel(): SettingsViewModel {
        every { languages.observeLanguage() } returns flowOf(AppLanguage.VIETNAMESE)
        return SettingsViewModel(FakeAuthRepository(), SavedStateHandle(), completions, languages).also { store.put(VIEW_MODEL_KEY, it) }
    }
}
