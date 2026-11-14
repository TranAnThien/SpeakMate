package com.project.speakmate.domain.repository

import com.project.speakmate.domain.model.VocabularyItem
import com.project.speakmate.domain.model.VocabularySaveResult
import kotlinx.coroutines.flow.Flow

interface SavedVocabularyRepository {
    fun getSavedVocabularies(): Flow<List<VocabularyItem>>
    suspend fun saveVocabulary(item: VocabularyItem): Result<VocabularySaveResult>
    suspend fun removeVocabulary(word: String, partOfSpeech: String): Result<Unit>
}