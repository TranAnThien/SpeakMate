package com.project.speakmate.ui.profile.savedvocab.state

import com.project.speakmate.domain.model.PronunciationPlaybackState
import com.project.speakmate.domain.model.SavedVocabularyIdentity
import com.project.speakmate.domain.model.VocabularyItem

data class SavedVocabUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val allVocabularies: List<VocabularyItem> = emptyList(),
    val filteredVocabularies: List<VocabularyItem> = emptyList(),
    val hasLoadError: Boolean = false,
    val hasDeleteError: Boolean = false,
    val failedDeletionIdentity: SavedVocabularyIdentity? = null,
    val hasLoaded: Boolean = false,
    val visibleCount: Int = 0,
    val deletingCount: Int = 0,
    val playback: PronunciationPlaybackState = PronunciationPlaybackState(),
    val pronunciationIdentity: SavedVocabularyIdentity? = null
) {}
