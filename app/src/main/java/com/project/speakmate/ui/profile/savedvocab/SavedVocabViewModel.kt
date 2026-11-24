package com.project.speakmate.ui.profile.savedvocab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.model.PronunciationState
import com.project.speakmate.domain.model.SavedVocabularyIdentity
import com.project.speakmate.domain.model.VocabularyItem
import com.project.speakmate.domain.repository.AuthRepository
import com.project.speakmate.domain.repository.PronunciationRepository
import com.project.speakmate.domain.repository.SavedVocabularyRepository
import com.project.speakmate.domain.rules.SavedVocabularyRules
import com.project.speakmate.ui.profile.savedvocab.state.SavedVocabUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SavedVocabViewModel
@Inject
constructor(
    private val savedVocabularyRepository: SavedVocabularyRepository,
    private val pronunciationRepository: PronunciationRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SavedVocabUiState())
    val uiState: StateFlow<SavedVocabUiState> = _uiState.asStateFlow()
    private var observationJob: Job? = null
    private var observedUserId: String? = null
    private var userRevision = 0L
    private val deletingIdentities = mutableSetOf<SavedVocabularyIdentity>()

    init {
        observeVocabulary()
        viewModelScope.launch {
            try {
                pronunciationRepository.playback.collect { playback ->
                    _uiState.update {
                        it.copy(
                            playback = playback,
                            pronunciationIdentity =
                                if (playback.status == PronunciationState.IDLE) {
                                    null
                                } else it.pronunciationIdentity
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        pronunciationIdentity = null,
                        playback = it.playback.copy(status = PronunciationState.FAILED)
                    )
                }
            }
        }
    }

    override fun onCleared() {
        pronunciationRepository.release()
        super.onCleared()
    }

    fun retry() {
        _uiState.update { it.copy(hasDeleteError = false) }
        observeVocabulary()
    }

    fun clearSearch() {
        updateSearchQuery("")
    }

    fun playWord(item: VocabularyItem) {
        playWord(item.word, item.partOfSpeech)
    }

    fun removeVocabulary(item: VocabularyItem) {
        removeVocabulary(item.word, item.partOfSpeech)
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { refresh(it.copy(searchQuery = query)) }
    }

    fun removeVocabulary(word: String, partOfSpeech: String) {
        val uid = observedUserId ?: return
        if (!isCurrentUser(uid)) return
        val revision = userRevision
        val identity = SavedVocabularyRules.getIdentity(word, partOfSpeech)
        val state = _uiState.value
        if (state.allVocabularies.none { SavedVocabularyRules.getIdentity(it) == identity } ||
            !deletingIdentities.add(identity))
            return
        if (state.pronunciationIdentity == identity) stopPronunciation()
        _uiState.update { refresh(it.copy(hasDeleteError = false, failedDeletionIdentity = null)) }
        viewModelScope.launch {
            try {
                savedVocabularyRepository.removeVocabularies(uid, listOf(identity)).getOrThrow()
                if (revision != userRevision || !isCurrentUser(uid)) return@launch
                deletingIdentities.remove(identity)
                _uiState.update {
                    refresh(
                        it.copy(
                            allVocabularies =
                                it.allVocabularies.filterNot { item ->
                                    SavedVocabularyRules.getIdentity(item) == identity
                                }
                        )
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (revision != userRevision || !isCurrentUser(uid)) return@launch
                deletingIdentities.remove(identity)
                _uiState.update { refresh(it.copy(hasDeleteError = true, failedDeletionIdentity = identity)) }
            }
        }
    }

    fun retryDelete() {
        val identity = _uiState.value.failedDeletionIdentity ?: return
        removeVocabulary(identity.word, identity.partOfSpeech)
    }

    fun leaveScreen() {
        stopPronunciation()
    }

    fun clearError() {
        _uiState.update { it.copy(hasDeleteError = false, failedDeletionIdentity = null) }
    }

    fun playWord(word: String, partOfSpeech: String = "") {
        val uid = observedUserId ?: return
        if (!isCurrentUser(uid) || word.isBlank()) return
        val identity = SavedVocabularyRules.getIdentity(word, partOfSpeech)
        if (_uiState.value.pronunciationIdentity != identity) pronunciationRepository.stop()
        _uiState.update { it.copy(pronunciationIdentity = identity) }
        pronunciationRepository.playText(word)
    }

    fun stopPronunciation() {
        pronunciationRepository.stop()
        _uiState.update { it.copy(pronunciationIdentity = null) }
    }

    private fun observeVocabulary() {
        observationJob?.cancel()
        _uiState.update { it.copy(isLoading = true, hasLoadError = false) }
        observationJob =
            viewModelScope.launch {
                try {
                    authRepository.observeSignedInUserId().distinctUntilChanged().collectLatest { uid ->
                        if (uid != observedUserId) {
                            userRevision++
                            deletingIdentities.clear()
                            pronunciationRepository.stop()
                            observedUserId = uid
                            _uiState.value = SavedVocabUiState(isLoading = uid != null)
                        }
                        if (uid == null) {
                            _uiState.value = SavedVocabUiState(isLoading = false)
                            return@collectLatest
                        }
                        try {
                            savedVocabularyRepository.observeSavedVocabularies().collect { vocabulary ->
                                if (!isCurrentUser(uid)) return@collect
                                _uiState.update {
                                    refresh(
                                        it.copy(
                                            isLoading = false,
                                            hasLoaded = true,
                                            hasLoadError = false,
                                            allVocabularies = vocabulary.toList()
                                        )
                                    )
                                }
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            if (isCurrentUser(uid)) _uiState.update { it.copy(isLoading = false, hasLoadError = true) }
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    leaveScreen()
                    val uid = observedUserId
                    if (uid != null && isCurrentUser(uid)) {
                        _uiState.update { it.copy(isLoading = false, hasLoadError = true) }
                    } else {
                        observedUserId = null
                        userRevision++
                        deletingIdentities.clear()
                        _uiState.value = SavedVocabUiState(isLoading = false, hasLoadError = true)
                    }
                }
            }
    }

    private fun isCurrentUser(uid: String): Boolean =
        try {
            observedUserId == uid && authRepository.requireCurrentUserId() == uid
        } catch (_: IllegalStateException) {
            false
        }

    private fun refresh(state: SavedVocabUiState): SavedVocabUiState {
        val excluded = deletingIdentities.toSet()
        val visible =
            SavedVocabularyRules.getVisibleItems(items = state.allVocabularies, excluded = excluded, query = "")
        return state.copy(
            visibleCount = visible.size,
            deletingCount = deletingIdentities.size,
            filteredVocabularies =
                SavedVocabularyRules.getVisibleItems(items = visible, excluded = emptySet(), query = state.searchQuery)
        )
    }
}
