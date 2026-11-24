package com.project.speakmate.ui.savedvocab

import com.project.speakmate.domain.model.LearningExample
import com.project.speakmate.domain.model.PronunciationPlaybackState
import com.project.speakmate.domain.model.VocabularyItem
import com.project.speakmate.domain.repository.PronunciationRepository
import com.project.speakmate.domain.repository.SavedVocabularyRepository
import com.project.speakmate.domain.rules.SavedVocabularyRules
import com.project.speakmate.test.FakeAuthRepository
import com.project.speakmate.test.MainDispatcherRule
import com.project.speakmate.ui.profile.savedvocab.SavedVocabViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SavedVocabViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository()
    private val clock = Clock.systemUTC()
    private val fakeVocabFlow = MutableStateFlow<List<VocabularyItem>>(emptyList())
    private lateinit var repository: SavedVocabularyRepository
    private lateinit var pronunciationRepository: PronunciationRepository
    private lateinit var viewModel: SavedVocabViewModel

    private fun makeItem(word: String, mean: String, pos: String = "noun") =
        VocabularyItem(word = word, mean = mean, phonetic = "", partOfSpeech = pos, example = LearningExample("", ""))

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        every { repository.observeSavedVocabularies() } returns fakeVocabFlow
        pronunciationRepository = mockk(relaxed = true)
        every { pronunciationRepository.playback } returns MutableStateFlow(PronunciationPlaybackState())
        viewModel =
            SavedVocabViewModel(
                savedVocabularyRepository = repository,
                pronunciationRepository = pronunciationRepository,
                authRepository = authRepository
            )
    }

    @Test
    fun initialUiStateHasIsLoadingTrueBeforeFirstEmission() {
        every { repository.observeSavedVocabularies() } returns emptyFlow()
        val pending =
            SavedVocabViewModel(
                savedVocabularyRepository = repository,
                pronunciationRepository = pronunciationRepository,
                authRepository = authRepository
            )
        assertTrue(pending.uiState.value.isLoading)
    }

    @Test
    fun uiStateIsLoadingFalseAfterFirstVocabEmission() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        fakeVocabFlow.emit(emptyList())
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun uiStateIsEmptyTrueWhenListIsEmptyAndNotLoading() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        fakeVocabFlow.emit(emptyList())
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.filteredVocabularies.isEmpty())
    }

    @Test
    fun updateSearchQueryFiltersByWordCaseInsensitive() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        fakeVocabFlow.emit(
            listOf(
                makeItem("resilience", "sự kiên cường"),
                makeItem("ubiquitous", "có mặt khắp nơi"),
                makeItem("implement", "triển khai")
            )
        )
        advanceUntilIdle()

        viewModel.updateSearchQuery("RESIL")
        advanceUntilIdle()

        val filtered = viewModel.uiState.value.filteredVocabularies
        assertEquals(1, filtered.size)
        assertEquals("resilience", filtered.first().word)
    }

    @Test
    fun updateSearchQueryFiltersByMeanCaseInsensitive() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        fakeVocabFlow.emit(listOf(makeItem("resilience", "sự kiên cường"), makeItem("ubiquitous", "có mặt khắp nơi")))
        advanceUntilIdle()

        viewModel.updateSearchQuery("kiên cường")
        advanceUntilIdle()

        val filtered = viewModel.uiState.value.filteredVocabularies
        assertEquals(1, filtered.size)
        assertEquals("resilience", filtered.first().word)
    }

    @Test
    fun blankSearchQueryShowsAllVocabularies() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        val items = listOf(makeItem("a", "alpha"), makeItem("b", "beta"))
        fakeVocabFlow.emit(items)
        advanceUntilIdle()

        viewModel.updateSearchQuery("a")
        advanceUntilIdle()
        viewModel.updateSearchQuery("")
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.filteredVocabularies.size)
    }

    @Test
    fun searchWithNoMatchReturnsEmptyFilteredList() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        fakeVocabFlow.emit(listOf(makeItem("resilience", "sự kiên cường")))
        advanceUntilIdle()

        viewModel.updateSearchQuery("xyz999")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.filteredVocabularies.isEmpty())
    }

    @Test
    fun removeVocabularyCallsRepositoryWithCorrectKeyPair() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        fakeVocabFlow.value = listOf(makeItem("test", "meaning"))
        advanceUntilIdle()
        coEvery {
            repository.removeVocabularies("user-1", listOf(SavedVocabularyRules.getIdentity("test", "noun")))
        } returns Result.success(Unit)

        viewModel.removeVocabulary("test", "noun")
        advanceUntilIdle()

        coVerify(exactly = 1) {
            repository.removeVocabularies("user-1", listOf(SavedVocabularyRules.getIdentity("test", "noun")))
        }
    }

    @Test
    fun removeVocabularySetsErrorMessageOnFailure() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        fakeVocabFlow.value = listOf(makeItem("test", "meaning"), makeItem(word = "x", mean = "meaning", pos = "y"))
        advanceUntilIdle()
        coEvery { repository.removeVocabularies(any(), any()) } returns Result.failure(Exception("Firestore error"))

        viewModel.removeVocabulary("test", "noun")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasDeleteError)
    }

    @Test
    fun clearErrorResetsErrorToNull() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        fakeVocabFlow.value = listOf(makeItem("test", "meaning"), makeItem(word = "x", mean = "meaning", pos = "y"))
        advanceUntilIdle()
        coEvery { repository.removeVocabularies(any(), any()) } returns Result.failure(Exception("err"))

        viewModel.removeVocabulary("x", "y")
        advanceUntilIdle()
        viewModel.clearError()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasDeleteError)
    }

    @Test
    fun playsTheSameWordOnEachTap() {
        viewModel.playWord("resilience")
        viewModel.playWord("resilience")
        verify(exactly = 2) { pronunciationRepository.playText("resilience") }
    }

    @Test
    fun failedListenerShowsErrorAndRetryStartsANewCollection() = runTest {
        every { repository.observeSavedVocabularies() } returns flow { throw IllegalStateException() }
        viewModel.retry()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.hasLoadError)
        every { repository.observeSavedVocabularies() } returns fakeVocabFlow
        viewModel.retry()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.hasLoadError)
        assertTrue(viewModel.uiState.value.filteredVocabularies.isEmpty())
    }
}
