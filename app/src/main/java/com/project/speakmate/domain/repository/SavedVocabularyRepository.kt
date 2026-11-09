package com.project.speakmate.domain.repository

import com.project.speakmate.domain.model.VocabularyItem
import com.project.speakmate.domain.model.VocabularySaveResult

interface SavedVocabularyRepository {
    suspend fun saveVocabulary(item: VocabularyItem): VocabularySaveResult
}
