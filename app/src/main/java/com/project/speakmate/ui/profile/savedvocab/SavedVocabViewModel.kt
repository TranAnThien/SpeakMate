package com.project.speakmate.ui.profile.savedvocab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.repository.SavedVocabularyRepository
import com.project.speakmate.ui.profile.savedvocab.state.SavedVocabUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavedVocabViewModel @Inject constructor(
    private val savedVocabularyRepository: SavedVocabularyRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)
    private val _pronunciationWord = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SavedVocabUiState> = combine(
        savedVocabularyRepository.getSavedVocabularies(),
        _searchQuery,
        _isLoading,
        _error
    ) { vocabList, query, loading, errorMsg ->
        val filtered = if (query.isBlank()) {
            vocabList
        } else {
            val lowerQuery = query.trim().lowercase()
            vocabList.filter {
                it.word.lowercase().contains(lowerQuery) ||
                        it.mean.lowercase().contains(lowerQuery)
            }
        }

        SavedVocabUiState(
            isLoading = false,
            searchQuery = query,
            allVocabularies = vocabList,
            filteredVocabularies = filtered,
            error = errorMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SavedVocabUiState()
    )

    /** Flow phát ra từ cần phát âm để Screen/Activity collect và gọi TTS */
    val pronunciationWord: StateFlow<String?> = _pronunciationWord

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun removeVocabulary(word: String, partOfSpeech: String) {
        viewModelScope.launch {
            val result = savedVocabularyRepository.removeVocabulary(word, partOfSpeech)
            result.onFailure { exception ->
                _error.value = exception.message
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    /**
     * Yêu cầu phát âm từ. Screen sẽ collect [pronunciationWord] để khởi động TextToSpeech.
     * Reset về null sau mỗi lần phát để tránh re-trigger khi recompose.
     */
    fun playPronunciation(word: String) {
        _pronunciationWord.value = word
    }

    fun onPronunciationConsumed() {
        _pronunciationWord.value = null
    }
}