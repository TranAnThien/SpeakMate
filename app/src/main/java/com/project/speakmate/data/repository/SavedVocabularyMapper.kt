package com.project.speakmate.data.repository

import com.project.speakmate.data.remote.firebase.FirestoreField
import com.project.speakmate.data.remote.firebase.FirestoreStorageSize
import com.project.speakmate.domain.model.LearningExample
import com.project.speakmate.domain.model.VocabularyItem
import com.project.speakmate.domain.model.VocabularySaveResult
import com.project.speakmate.domain.rules.SavedVocabularyRules
import com.project.speakmate.utils.extension.defaultEmpty
import java.util.Locale

object SavedVocabularyMapper {
    fun toItem(value: Any?): VocabularyItem? {
        val map = value as? Map<*, *> ?: return null
        val word = map[FirestoreField.WORD] as? String ?: return null
        val mean = map[FirestoreField.MEAN] as? String ?: return null
        if (word.isBlank() || mean.isBlank()) return null
        val phonetic = map[FirestoreField.PHONETIC]
        val partOfSpeech = map[FirestoreField.PART_OF_SPEECH]
        val example = map[FirestoreField.EXAMPLE]
        if ((phonetic != null && phonetic !is String) || (partOfSpeech != null && partOfSpeech !is String) || (example != null && example !is Map<*, *>)) return null
        val examples = example as? Map<*, *>
        val sentence = examples?.get(FirestoreField.SENTENCE)
        val translate = examples?.get(FirestoreField.TRANSLATE)
        if ((sentence != null && sentence !is String) || (translate != null && translate !is String)) return null
        return VocabularyItem(word, mean, (phonetic as? String).defaultEmpty(), (partOfSpeech as? String).defaultEmpty(),
            LearningExample((sentence as? String).defaultEmpty(), (translate as? String).defaultEmpty()))
    }

    fun toRaw(item: VocabularyItem): Map<String, Any?> = mapOf(
        FirestoreField.WORD to item.word, FirestoreField.MEAN to item.mean,
        FirestoreField.PHONETIC to item.phonetic, FirestoreField.PART_OF_SPEECH to item.partOfSpeech,
        FirestoreField.EXAMPLE to mapOf(FirestoreField.SENTENCE to item.example.sentence, FirestoreField.TRANSLATE to item.example.translate)
    )

    fun prepareSave(fields: Map<String, Any?>, path: String, item: VocabularyItem): Pair<List<Any?>, VocabularySaveResult> {
        val normalized = SavedVocabularyRules.normalize(item)
        val current = getRaw(fields)
        val duplicate = current.indexOfFirst { matches(it, normalized.word, normalized.partOfSpeech) }
        if (duplicate >= 0) {
            val moved = current.toMutableList()
            val existing = moved.removeAt(duplicate)
            moved.add(0, existing)
            return moved.toList() to VocabularySaveResult.MOVED_TO_TOP
        }
        val raw = toRaw(normalized)
        val base = fields.toMutableMap().also { it[FirestoreField.SAVED_VOCABULARIES] = emptyList<Any>() }
        val sizes = current.map(FirestoreStorageSize::getValueBytes)
        val removable = current.indices.filter { toItem(current[it]) != null }.toSet()
        val removed = SavedVocabularyRules.getRemovalIndices(sizes, removable, FirestoreStorageSize.getValueBytes(raw), FirestoreStorageSize.getDocumentBytes(path, base)).toSet()
        return (listOf(raw) + current.filterIndexed { index, _ -> index !in removed }) to
            if (removed.isEmpty()) VocabularySaveResult.SAVED else VocabularySaveResult.SAVED_REPLACING_OLDEST
    }

    fun prepareRemoval(fields: Map<String, Any?>, word: String, partOfSpeech: String): List<Any?> =
        getRaw(fields).filterNot { matches(it, word.trim().lowercase(Locale.ROOT), partOfSpeech.trim()) }

    fun getRaw(fields: Map<String, Any?>): List<Any?> {
        if (!fields.containsKey(FirestoreField.SAVED_VOCABULARIES)) return emptyList()
        val value = fields[FirestoreField.SAVED_VOCABULARIES]
        check(value is List<*>)
        return value.toList()
    }

    private fun matches(value: Any?, word: String, partOfSpeech: String): Boolean {
        val item = toItem(value) ?: return false
        return item.word.trim().lowercase(Locale.ROOT) == word && item.partOfSpeech.trim() == partOfSpeech
    }
}
