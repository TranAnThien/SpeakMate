package com.project.speakmate.ui.profile.savedvocab

import com.project.speakmate.utils.AppConstant
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.repository.PronunciationRepository
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
    private val savedVocabularyRepository: SavedVocabularyRepository,
    private val pronunciationRepository: PronunciationRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SavedVocabUiState> = combine(
        savedVocabularyRepository.observeSavedVocabularies(),
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
        started = SharingStarted.WhileSubscribed(AppConstant.STATE_SUBSCRIPTION_TIMEOUT_MILLIS),
        initialValue = SavedVocabUiState()
    )

    override fun onCleared() {
        pronunciationRepository.release()
        super.onCleared()
    }

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

    fun playWord(word: String) {
        pronunciationRepository.playText(word)
    }

    fun stopPronunciation() {
        pronunciationRepository.stop()
    }
}
