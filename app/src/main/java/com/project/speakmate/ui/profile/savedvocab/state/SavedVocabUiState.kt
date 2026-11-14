package com.project.speakmate.ui.profile.savedvocab.state

import androidx.compose.runtime.Immutable
import com.project.speakmate.domain.model.VocabularyItem

@Immutable
data class SavedVocabUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val allVocabularies: List<VocabularyItem> = emptyList(),
    val filteredVocabularies: List<VocabularyItem> = emptyList(),
    val error: String? = null
) {
    val isEmpty: Boolean
        get() = !isLoading && filteredVocabularies.isEmpty()
}

