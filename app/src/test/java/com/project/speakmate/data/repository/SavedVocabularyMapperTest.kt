package com.project.speakmate.data.repository

import com.project.speakmate.data.remote.firebase.FirestoreField
import com.project.speakmate.data.remote.firebase.FirestoreStorageSize
import com.project.speakmate.domain.model.LearningExample
import com.project.speakmate.domain.model.VocabularyItem
import com.project.speakmate.domain.model.VocabularySaveResult
import com.project.speakmate.domain.rules.SavedVocabularyRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedVocabularyMapperTest {
    companion object {
        private const val USER_PATH = "users/student"
    }

    private val word = VocabularyItem("Coffee", "Cà phê", "", "noun", LearningExample("", ""))
    private val invalid = mapOf(FirestoreField.WORD to "broken", FirestoreField.MEAN to 1)

    @Test
    fun savingAndDeletingKeepRawInvalidValuesAndUnknownFields() {
        val raw = SavedVocabularyMapper.toRaw(word.copy(word = "old")) + mapOf("unknown" to "keep")
        val fields = mapOf(FirestoreField.SAVED_VOCABULARIES to listOf(invalid, raw))
        val saved = SavedVocabularyMapper.prepareSave(fields, USER_PATH, word)
        assertSame(invalid, saved.first[1])
        assertSame(raw, saved.first[2])
        val removed = SavedVocabularyMapper.prepareRemoval(mapOf(FirestoreField.SAVED_VOCABULARIES to saved.first), "coffee", "noun")
        assertEquals(listOf(invalid, raw), removed)
        assertSame(raw, removed[1])
    }

    @Test
    fun duplicateMovesTheOriginalRawEntryWithoutEvictionOrRewriting() {
        val raw = SavedVocabularyMapper.toRaw(word.copy(word = "coffee")) + mapOf("unknown" to "keep")
        val fields = mapOf(FirestoreField.SAVED_VOCABULARIES to listOf(invalid, raw), "largeOtherField" to "x".repeat(SavedVocabularyRules.MAX_DOCUMENT_BYTES.toInt()))
        val saved = SavedVocabularyMapper.prepareSave(fields, USER_PATH, word.copy(mean = "New definition"))
        assertEquals(VocabularySaveResult.MOVED_TO_TOP, saved.second)
        assertEquals(2, saved.first.size)
        assertSame(raw, saved.first[0])
        assertSame(invalid, saved.first[1])
    }

    @Test
    fun budgetEvictionSkipsInvalidTailAndRemovesSeveralOldWords() {
        val old = SavedVocabularyMapper.toRaw(word.copy(word = "old")) + mapOf("unknown" to "x".repeat(SavedVocabularyRules.MAX_ARRAY_BYTES.toInt() / 2))
        val fields = mapOf(FirestoreField.SAVED_VOCABULARIES to listOf(old, old, old, invalid))
        val saved = SavedVocabularyMapper.prepareSave(fields, USER_PATH, word)
        assertEquals(VocabularySaveResult.SAVED_REPLACING_OLDEST, saved.second)
        assertTrue(saved.first.size < 5)
        assertSame(invalid, saved.first.last())
        assertTrue(FirestoreStorageSize.getValueBytes(saved.first) <= SavedVocabularyRules.MAX_ARRAY_BYTES)
        assertEquals(4, (fields[FirestoreField.SAVED_VOCABULARIES] as List<*>).size)
    }

    @Test
    fun deletingAnItemDoesNotDeleteAnInvalidItemWithTheSameWord() {
        val valid = SavedVocabularyMapper.toRaw(word.copy(word = "broken"))
        val result = SavedVocabularyMapper.prepareRemoval(mapOf(FirestoreField.SAVED_VOCABULARIES to listOf(valid, invalid)), "BROKEN", "noun")
        assertEquals(listOf(invalid), result)
    }

    @Test
    fun malformedOptionalFieldsDoNotBecomeValidRemovableEntries() {
        assertNull(SavedVocabularyMapper.toItem(SavedVocabularyMapper.toRaw(word) + mapOf(FirestoreField.PHONETIC to 1)))
        assertNull(SavedVocabularyMapper.toItem(invalid))
    }

    @Test
    fun sizeIncludesUtf8FieldNamesDocumentPathAndNestedMaps() {
        val fields = mapOf("x" to listOf("é", 1L, true, null), "map" to mapOf("v" to "a"))
        assertEquals(32L + 2L + 3L + 8L + 1L + 1L + 4L + 32L + 2L + 2L, FirestoreStorageSize.getValueBytes(fields))
        assertEquals(16L + 6L + 8L + FirestoreStorageSize.getValueBytes(fields), FirestoreStorageSize.getDocumentBytes(USER_PATH, fields))
    }
    @Test
    fun groupRemovalIsRepeatSafeAndPreservesLatestUnrelatedRawContentAndOrder() {
        val coffee = SavedVocabularyMapper.toRaw(word)
        val verb = SavedVocabularyMapper.toRaw(word.copy(word = "run", partOfSpeech = "verb"))
        val noun = SavedVocabularyMapper.toRaw(word.copy(word = "run", partOfSpeech = "noun", mean = "latest definition")) + mapOf("newField" to true)
        val tea = SavedVocabularyMapper.toRaw(word.copy(word = "tea"))
        val fields = mapOf(FirestoreField.SAVED_VOCABULARIES to listOf(tea, invalid, coffee, noun, verb, coffee))
        val targets = listOf(SavedVocabularyRules.getIdentity(" coffee ", " noun "), SavedVocabularyRules.getIdentity("RUN", "verb"))
        val removed = SavedVocabularyMapper.prepareRemoval(fields, targets)
        assertEquals(listOf(tea, invalid, noun), removed)
        assertSame(noun, removed[2])
        assertEquals(removed, SavedVocabularyMapper.prepareRemoval(mapOf(FirestoreField.SAVED_VOCABULARIES to removed), targets))
    }

    @Test
    fun emptyGroupDoesNotRewriteOrRemoveAnyRawEntry() {
        val raw = SavedVocabularyMapper.toRaw(word)
        val fields = mapOf(FirestoreField.SAVED_VOCABULARIES to listOf(invalid, raw))
        val removed = SavedVocabularyMapper.prepareRemoval(fields, emptyList())
        assertEquals(listOf(invalid, raw), removed)
        assertSame(raw, removed[1])
    }
}
