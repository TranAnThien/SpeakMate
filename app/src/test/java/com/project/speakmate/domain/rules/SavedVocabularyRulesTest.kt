package com.project.speakmate.domain.rules

import com.project.speakmate.domain.model.InvalidVocabularyException
import com.project.speakmate.domain.model.LearningExample
import com.project.speakmate.domain.model.VocabularyItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedVocabularyRulesTest {
    private val word = VocabularyItem("Coffee", "Cà phê", "", "noun", LearningExample("", ""))

    @Test
    fun fieldLimitsRejectEveryOversizedFieldWithoutTruncating() {
        val oversized = listOf(
            word.copy(word = "a".repeat(SavedVocabularyRules.MAX_WORD_LENGTH + 1)),
            word.copy(phonetic = "a".repeat(SavedVocabularyRules.MAX_PHONETIC_LENGTH + 1)),
            word.copy(partOfSpeech = "a".repeat(SavedVocabularyRules.MAX_PART_OF_SPEECH_LENGTH + 1)),
            word.copy(mean = "a".repeat(SavedVocabularyRules.MAX_TEXT_LENGTH + 1)),
            word.copy(example = LearningExample("a".repeat(SavedVocabularyRules.MAX_TEXT_LENGTH + 1), "")),
            word.copy(example = LearningExample("a", "a".repeat(SavedVocabularyRules.MAX_TEXT_LENGTH + 1)))
        )
        oversized.forEach {
            assertFalse(SavedVocabularyRules.hasValidFields(it))
            assertEquals(InvalidVocabularyException.Reason.FIELD_TOO_LONG, rejection { SavedVocabularyRules.normalize(it) })
        }
    }

    @Test
    fun unicodeLimitsCountCodePointsAndNormalizationKeepsLowercaseWords() {
        assertTrue(SavedVocabularyRules.hasValidFields(word.copy(word = "😀".repeat(SavedVocabularyRules.MAX_WORD_LENGTH))))
        assertEquals("coffee", SavedVocabularyRules.normalize(word.copy(word = " COFFEE ")).word)
        assertEquals(InvalidVocabularyException.Reason.INVALID_CONTENT, rejection { SavedVocabularyRules.normalize(word.copy(mean = " ")) })
    }

    @Test
    fun fullListRemovesOnlyTheOldestRemovableItem() {
        val sizes = List(SavedVocabularyRules.MAX_ITEM_COUNT) { 1L }
        assertEquals(listOf(sizes.lastIndex), SavedVocabularyRules.getRemovalIndices(sizes, sizes.indices.toSet(), 1, 0))
    }

    @Test
    fun arrayBudgetMayRemoveSeveralOldestItems() {
        val size = SavedVocabularyRules.MAX_ARRAY_BYTES / 2
        assertEquals(listOf(2, 1), SavedVocabularyRules.getRemovalIndices(listOf(size, size, size), setOf(0, 1, 2), size, 0))
    }

    @Test
    fun documentBudgetIncludesOtherUserFields() {
        val base = SavedVocabularyRules.MAX_DOCUMENT_BYTES - 1
        assertEquals(listOf(1, 0), SavedVocabularyRules.getRemovalIndices(listOf(1L, 1L), setOf(0, 1), 1, base))
    }

    @Test
    fun invalidEntriesAreProtectedAndCanMakeSavingImpossible() {
        val size = SavedVocabularyRules.MAX_ARRAY_BYTES
        assertEquals(InvalidVocabularyException.Reason.STORAGE_BUDGET, rejection {
            SavedVocabularyRules.getRemovalIndices(listOf(size), emptySet(), 1, 0)
        })
    }

    private fun rejection(operation: () -> Unit): InvalidVocabularyException.Reason {
        try { operation() } catch (failure: InvalidVocabularyException) { return failure.reason }
        throw AssertionError()
    }
}
