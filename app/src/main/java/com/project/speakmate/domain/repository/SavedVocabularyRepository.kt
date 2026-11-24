package com.project.speakmate.domain.repository

import com.project.speakmate.domain.model.VocabularyItem
import com.project.speakmate.domain.model.VocabularySaveResult
import com.project.speakmate.domain.model.SavedVocabularyIdentity
import kotlinx.coroutines.flow.Flow

interface SavedVocabularyRepository {
    fun observeSavedVocabularies(): Flow<List<VocabularyItem>>
    suspend fun saveVocabulary(item: VocabularyItem): Result<VocabularySaveResult>
    suspend fun removeVocabulary(word: String, partOfSpeech: String): Result<Unit>
    suspend fun removeVocabularies(userId: String, identities: List<SavedVocabularyIdentity>): Result<Unit>
}
