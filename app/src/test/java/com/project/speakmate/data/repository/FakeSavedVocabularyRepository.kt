package com.project.speakmate.data.repository

import com.project.speakmate.domain.model.SavedVocabularyIdentity
import com.project.speakmate.domain.rules.SavedVocabularyRules
import com.project.speakmate.domain.model.VocabularyItem
import com.project.speakmate.domain.model.VocabularySaveResult
import com.project.speakmate.domain.repository.SavedVocabularyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeSavedVocabularyRepository : SavedVocabularyRepository {

    private val list = mutableListOf<VocabularyItem>()

    override fun observeSavedVocabularies(): Flow<List<VocabularyItem>> =
        flowOf(list.toList())

    override suspend fun saveVocabulary(item: VocabularyItem): Result<VocabularySaveResult> {
        val normalizedWord = item.word.trim().lowercase()
        val normalizedMean = item.mean.trim()
        if (normalizedWord.isBlank() || normalizedMean.isBlank()) {
            return Result.failure(Exception("Word and mean cannot be empty"))
        }
        val validItem = item.copy(word = normalizedWord, mean = normalizedMean)
        val existingIndex = list.indexOfFirst {
            it.word == validItem.word && it.partOfSpeech == validItem.partOfSpeech
        }
        return if (existingIndex != -1) {
            list.removeAt(existingIndex)
            list.add(0, validItem)
            Result.success(VocabularySaveResult.MOVED_TO_TOP)
        } else {
            list.add(0, validItem)
            if (list.size > 1000) {
                list.removeLast()
                Result.success(VocabularySaveResult.SAVED_REPLACING_OLDEST)
            } else {
                Result.success(VocabularySaveResult.SAVED)
            }
        }
    }

    override suspend fun removeVocabulary(word: String, partOfSpeech: String): Result<Unit> {
        list.removeAll { it.word == word.trim().lowercase() && it.partOfSpeech == partOfSpeech.trim() }
        return Result.success(Unit)
    }
    override suspend fun removeVocabularies(userId: String, identities: List<SavedVocabularyIdentity>): Result<Unit> {
        val targets = identities.map { SavedVocabularyRules.getIdentity(it.word, it.partOfSpeech) }.toSet()
        list.removeAll { SavedVocabularyRules.getIdentity(it) in targets }
        return Result.success(Unit)
    }
}
