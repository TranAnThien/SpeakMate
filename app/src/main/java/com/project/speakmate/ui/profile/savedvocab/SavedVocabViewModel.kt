package com.project.speakmate.ui.profile.savedvocab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.model.VocabularyItem
import com.project.speakmate.domain.repository.PronunciationRepository
import com.project.speakmate.domain.repository.SavedVocabularyRepository
import com.project.speakmate.ui.profile.savedvocab.state.SavedVocabUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SavedVocabViewModel @Inject constructor(
    private val savedVocabularyRepository: SavedVocabularyRepository,
    private val pronunciationRepository: PronunciationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SavedVocabUiState())
    val uiState: StateFlow<SavedVocabUiState> = _uiState.asStateFlow()
    private var observationJob: Job? = null

    init {
        observeVocabulary()
    }

    override fun onCleared() {
        pronunciationRepository.release()
        super.onCleared()
    }

    fun retry() {
        observeVocabulary()
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query, filteredVocabularies = filter(it.allVocabularies, query)) }
    }

    fun removeVocabulary(word: String, partOfSpeech: String) {
        viewModelScope.launch {
            try {
                savedVocabularyRepository.removeVocabulary(word, partOfSpeech).getOrThrow()
                _uiState.update { it.copy(hasDeleteError = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(hasDeleteError = true) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(hasDeleteError = false) }
    }

    fun playWord(word: String) {
        pronunciationRepository.playText(word)
    }

    fun stopPronunciation() {
        pronunciationRepository.stop()
    }

    private fun observeVocabulary() {
        observationJob?.cancel()
        _uiState.update { it.copy(isLoading = true, hasLoadError = false) }
        observationJob = viewModelScope.launch {
            try {
                savedVocabularyRepository.observeSavedVocabularies().collect { vocabulary ->
                    val snapshot = vocabulary.toList()
                    _uiState.update {
                        it.copy(isLoading = false, hasLoadError = false, allVocabularies = snapshot, filteredVocabularies = filter(snapshot, it.searchQuery))
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, hasLoadError = true, allVocabularies = emptyList(), filteredVocabularies = emptyList()) }
            }
        }
    }

    private fun filter(items: List<VocabularyItem>, query: String): List<VocabularyItem> {
        val normalized = query.trim().lowercase(Locale.ROOT)
        return if (normalized.isEmpty()) items else items.filter {
            it.word.lowercase(Locale.ROOT).contains(normalized) || it.mean.lowercase(Locale.ROOT).contains(normalized)
        }
    }
}
