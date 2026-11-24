package com.project.speakmate.domain.rules

import com.project.speakmate.domain.model.InvalidVocabularyException
import com.project.speakmate.domain.model.LearningExample
import com.project.speakmate.domain.model.VocabularyItem
import com.project.speakmate.domain.model.SavedVocabularyIdentity
import java.util.Locale

object SavedVocabularyRules {
    const val MAX_WORD_LENGTH = 128
    const val MAX_PHONETIC_LENGTH = 256
    const val MAX_PART_OF_SPEECH_LENGTH = 64
    const val MAX_TEXT_LENGTH = 2_048
    const val MAX_ITEM_COUNT = 1_000
    const val MAX_ARRAY_BYTES = 512L * 1_024L
    const val MAX_DOCUMENT_BYTES = 768L * 1_024L

    fun getIdentity(word: String, partOfSpeech: String): SavedVocabularyIdentity =
        SavedVocabularyIdentity(word.trim().lowercase(Locale.ROOT), partOfSpeech.trim())

    fun getIdentity(item: VocabularyItem): SavedVocabularyIdentity = getIdentity(item.word, item.partOfSpeech)

    fun getVisibleItems(items: List<VocabularyItem>, excluded: Set<SavedVocabularyIdentity>, query: String): List<VocabularyItem> {
        val normalized = query.trim().lowercase(Locale.ROOT)
        return items.distinctBy(::getIdentity).filter { item ->
            getIdentity(item) !in excluded && (normalized.isEmpty() ||
                item.word.lowercase(Locale.ROOT).contains(normalized) || item.mean.lowercase(Locale.ROOT).contains(normalized))
        }
    }

    fun hasValidFields(item: VocabularyItem): Boolean = item.word.isNotBlank() && item.mean.isNotBlank() &&
        hasLength(item.word, MAX_WORD_LENGTH) && hasLength(item.word.trim().lowercase(Locale.ROOT), MAX_WORD_LENGTH) && hasLength(item.phonetic, MAX_PHONETIC_LENGTH) &&
        hasLength(item.partOfSpeech, MAX_PART_OF_SPEECH_LENGTH) && hasLength(item.mean, MAX_TEXT_LENGTH) &&
        hasLength(item.example.sentence, MAX_TEXT_LENGTH) && hasLength(item.example.translate, MAX_TEXT_LENGTH)

    fun normalize(item: VocabularyItem): VocabularyItem {
        if (item.word.isBlank() || item.mean.isBlank()) throw InvalidVocabularyException(InvalidVocabularyException.Reason.INVALID_CONTENT)
        if (!hasValidFields(item)) throw InvalidVocabularyException(InvalidVocabularyException.Reason.FIELD_TOO_LONG)
        return item.copy(
            word = item.word.trim().lowercase(Locale.ROOT), mean = item.mean.trim(),
            phonetic = item.phonetic.trim(), partOfSpeech = item.partOfSpeech.trim(),
            example = if (item.example.sentence.isBlank()) LearningExample("", "") else item.example
        )
    }

    fun getRemovalIndices(sizes: List<Long>, removableIndices: Set<Int>, newSizeBytes: Long, documentBaseBytes: Long): List<Int> {
        require(newSizeBytes >= 0 && documentBaseBytes >= 0 && sizes.all { it >= 0 })
        var arrayBytes = sizes.sum() + newSizeBytes
        var itemCount = sizes.size + 1
        val removed = mutableListOf<Int>()
        for (index in sizes.indices.reversed()) {
            if (fits(itemCount, arrayBytes, documentBaseBytes)) break
            if (index in removableIndices) {
                removed += index
                arrayBytes -= sizes[index]
                itemCount--
            }
        }
        if (!fits(itemCount, arrayBytes, documentBaseBytes)) throw InvalidVocabularyException(InvalidVocabularyException.Reason.STORAGE_BUDGET)
        return removed.toList()
    }

    private fun hasLength(value: String, limit: Int): Boolean =
        value.length <= limit * Character.charCount(Character.MAX_CODE_POINT) &&
            value.codePointCount(0, value.length) <= limit

    private fun fits(count: Int, arrayBytes: Long, baseBytes: Long): Boolean =
        count <= MAX_ITEM_COUNT && arrayBytes <= MAX_ARRAY_BYTES && baseBytes + arrayBytes <= MAX_DOCUMENT_BYTES
}
