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

    @Test
    fun exactArrayAndDocumentBudgetsDoNotEvictExistingData() {
        val base = SavedVocabularyRules.MAX_DOCUMENT_BYTES - SavedVocabularyRules.MAX_ARRAY_BYTES
        assertEquals(emptyList<Int>(), SavedVocabularyRules.getRemovalIndices(emptyList(), emptySet(), SavedVocabularyRules.MAX_ARRAY_BYTES, base))
        assertEquals(InvalidVocabularyException.Reason.STORAGE_BUDGET, rejection {
            SavedVocabularyRules.getRemovalIndices(emptyList(), emptySet(), SavedVocabularyRules.MAX_ARRAY_BYTES, base + 1)
        })
        assertEquals(InvalidVocabularyException.Reason.STORAGE_BUDGET, rejection {
            SavedVocabularyRules.getRemovalIndices(emptyList(), emptySet(), SavedVocabularyRules.MAX_ARRAY_BYTES + 1, 0)
        })
    }

    @Test
    fun evictionSkipsAnOlderProtectedEntryAndRemovesTheOldestValidOne() {
        val size = SavedVocabularyRules.MAX_ARRAY_BYTES / 2 + 1
        assertEquals(listOf(0), SavedVocabularyRules.getRemovalIndices(listOf(size, size), setOf(0), 1, 0))
    }

    @Test
    fun lowercaseExpansionMustStillFitTheWordLimit() {
        val input = word.copy(word = "İ".repeat(SavedVocabularyRules.MAX_WORD_LENGTH))
        assertFalse(SavedVocabularyRules.hasValidFields(input))
        assertEquals(InvalidVocabularyException.Reason.FIELD_TOO_LONG, rejection { SavedVocabularyRules.normalize(input) })
    }

    @Test
    fun normalizationTrimsOptionalFieldsAndClearsTranslationWithoutAnExample() {
        val normalized = SavedVocabularyRules.normalize(word.copy(
            word = " COFFEE ", mean = " Drink ", phonetic = " /coffee/ ", partOfSpeech = " noun ",
            example = LearningExample(" ", "orphan translation")
        ))
        assertEquals(VocabularyItem("coffee", "Drink", "/coffee/", "noun", LearningExample("", "")), normalized)
        assertEquals(InvalidVocabularyException.Reason.INVALID_CONTENT, rejection { SavedVocabularyRules.normalize(word.copy(word = " ")) })
    }

    @Test
    fun identityNormalizesWordAndWhitespaceWithoutMergingDifferentPartsOfSpeech() {
        assertEquals(SavedVocabularyRules.getIdentity("COFFEE", "noun"), SavedVocabularyRules.getIdentity(" coffee ", " noun "))
        assertFalse(SavedVocabularyRules.getIdentity("run", "noun") == SavedVocabularyRules.getIdentity("run", "verb"))
        assertFalse(SavedVocabularyRules.getIdentity("run", "noun") == SavedVocabularyRules.getIdentity("run", "Noun"))
    }

    @Test
    fun visibleItemsDedupeByIdentityAndSearchBothWordAndMeaning() {
        val duplicate = word.copy(word = " COFFEE ", mean = "changed")
        val verb = word.copy(word = "run", partOfSpeech = "verb", mean = "Chạy")
        val source = listOf(word, duplicate, verb)
        assertEquals(listOf(word, verb), SavedVocabularyRules.getVisibleItems(source, emptySet(), ""))
        assertEquals(listOf(word), SavedVocabularyRules.getVisibleItems(source, emptySet(), "COF"))
        assertEquals(listOf(verb), SavedVocabularyRules.getVisibleItems(source, emptySet(), " CHẠY "))
    }

    @Test
    fun removingAnExclusionUsesTheLatestSnapshotOrderAndCurrentQuery() {
        val next = word.copy(word = "tea", mean = "Trà mới")
        val updated = word.copy(mean = "Cà phê mới")
        val latest = listOf(next, updated)
        val hidden = setOf(SavedVocabularyRules.getIdentity(word))
        assertTrue(SavedVocabularyRules.getVisibleItems(latest, hidden, "phê").isEmpty())
        assertEquals(listOf(updated), SavedVocabularyRules.getVisibleItems(latest, emptySet(), "phê"))
        assertEquals(latest, SavedVocabularyRules.getVisibleItems(latest, emptySet(), ""))
    }


    @Test
    fun removingTheExclusionDoesNotResurrectItemsDeletedFromTheLatestSnapshot() {
        val hidden = setOf(SavedVocabularyRules.getIdentity(word))
        assertTrue(SavedVocabularyRules.getVisibleItems(emptyList(), hidden, "").isEmpty())
        assertTrue(SavedVocabularyRules.getVisibleItems(emptyList(), emptySet(), "").isEmpty())
    }

    private fun rejection(operation: () -> Unit): InvalidVocabularyException.Reason {
        try { operation() } catch (failure: InvalidVocabularyException) { return failure.reason }
        throw AssertionError()
    }
}
