package com.project.speakmate.ui.savedvocab

import com.project.speakmate.domain.model.LearningExample
import com.project.speakmate.domain.model.VocabularyItem
import com.project.speakmate.domain.model.VocabularySaveResult
import com.project.speakmate.domain.repository.SavedVocabularyRepository
import com.project.speakmate.test.MainDispatcherRule
import com.project.speakmate.ui.profile.savedvocab.SavedVocabViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SavedVocabViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeVocabFlow = MutableStateFlow<List<VocabularyItem>>(emptyList())
    private lateinit var repository: SavedVocabularyRepository
    private lateinit var viewModel: SavedVocabViewModel

    private fun makeItem(word: String, mean: String, pos: String = "noun") = VocabularyItem(
        word = word,
        mean = mean,
        phonetic = "",
        partOfSpeech = pos,
        example = LearningExample("", "")
    )

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        io.mockk.every { repository.getSavedVocabularies() } returns fakeVocabFlow
        viewModel = SavedVocabViewModel(repository)
    }

    // ─── Trạng thái ban đầu ───────────────────────────────────────────────────

    @Test
    fun `initial uiState has isLoading true before first emission`() {
        // initialValue của stateIn được set isLoading = true
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `uiState isLoading false after first vocab emission`() = runTest {
        fakeVocabFlow.emit(emptyList())
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `uiState isEmpty true when list is empty and not loading`() = runTest {
        fakeVocabFlow.emit(emptyList())
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isEmpty)
    }

    // ─── Lọc tìm kiếm ────────────────────────────────────────────────────────

    @Test
    fun `updateSearchQuery filters by word (case insensitive)`() = runTest {
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
    fun `updateSearchQuery filters by mean (case insensitive)`() = runTest {
        fakeVocabFlow.emit(
            listOf(
                makeItem("resilience", "sự kiên cường"),
                makeItem("ubiquitous", "có mặt khắp nơi")
            )
        )
        advanceUntilIdle()

        viewModel.updateSearchQuery("kiên cường")
        advanceUntilIdle()

        val filtered = viewModel.uiState.value.filteredVocabularies
        assertEquals(1, filtered.size)
        assertEquals("resilience", filtered.first().word)
    }

    @Test
    fun `blank search query shows all vocabularies`() = runTest {
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
    fun `search with no match returns empty filtered list`() = runTest {
        fakeVocabFlow.emit(listOf(makeItem("resilience", "sự kiên cường")))
        advanceUntilIdle()

        viewModel.updateSearchQuery("xyz999")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.filteredVocabularies.isEmpty())
    }

    // ─── Xóa từ ──────────────────────────────────────────────────────────────

    @Test
    fun `removeVocabulary calls repository with correct key pair`() = runTest {
        coEvery { repository.removeVocabulary("test", "noun") } returns Result.success(Unit)

        viewModel.removeVocabulary("test", "noun")
        advanceUntilIdle()

        io.mockk.coVerify(exactly = 1) { repository.removeVocabulary("test", "noun") }
    }

    @Test
    fun `removeVocabulary sets error message on failure`() = runTest {
        coEvery { repository.removeVocabulary(any(), any()) } returns
                Result.failure(Exception("Firestore error"))

        viewModel.removeVocabulary("test", "noun")
        advanceUntilIdle()

        assertEquals("Firestore error", viewModel.uiState.value.error)
    }

    @Test
    fun `clearError resets error to null`() = runTest {
        coEvery { repository.removeVocabulary(any(), any()) } returns
                Result.failure(Exception("err"))

        viewModel.removeVocabulary("x", "y")
        advanceUntilIdle()
        viewModel.clearError()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
    }

    // ─── Phát âm ─────────────────────────────────────────────────────────────

    @Test
    fun `playPronunciation emits word to pronunciationWord flow`() {
        viewModel.playPronunciation("resilience")
        assertEquals("resilience", viewModel.pronunciationWord.value)
    }

    @Test
    fun `onPronunciationConsumed resets pronunciationWord to null`() {
        viewModel.playPronunciation("resilience")
        viewModel.onPronunciationConsumed()
        assertNull(viewModel.pronunciationWord.value)
    }
}

