package com.project.speakmate.data.repository

import com.project.speakmate.domain.model.LearningExample
import com.project.speakmate.domain.model.RequiredAction
import com.project.speakmate.domain.model.UnitPreparation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TrainingContentMapperTest {
    private val mapper = TrainingContentMapper()

    @Test
    fun oneValidPreparationSectionAllowsTheOthersToBeEmpty() {
        val vocabulary = mapper.toVocabulary(TrainingContentTestFixtures.getDocument(TrainingContentTestFixtures.getVocabulary()), "vocabulary")
        mapper.validatePreparation(UnitPreparation(listOf(vocabulary), emptyList(), emptyList()))
    }

    @Test(expected = IllegalStateException::class)
    fun entirelyMissingPreparationIsInvalidContent() {
        TrainingContentMapper().validatePreparation(UnitPreparation(emptyList(), emptyList(), emptyList()))
    }

    @Test
    fun missingOrMalformedOptionalPreparationSectionsStayEmpty() {
        listOf(null, "invalid", emptyMap<String, Any>(), mapOf("vocabulary" to "invalid")).forEach { preparation ->
            assertEquals(emptyList<String>(), mapper.getPreparationReferenceIds(preparation, "vocabulary"))
        }
    }

    @Test
    fun optionalReferencesKeepValidIdsInOrderAndRemoveDuplicates() {
        val preparation = mapOf("vocabulary" to listOf(null, "invalid", emptyMap<String, Any>(), mapOf("id" to ""), mapOf("id" to 3), mapOf("id" to "one"),
            mapOf("id" to "one"), mapOf("id" to "two")))
        assertEquals(listOf("one", "two"), mapper.getPreparationReferenceIds(preparation, "vocabulary"))
        assertEquals(emptyList<String>(), mapper.getPreparationReferenceIds(preparation, "grammarPoints"))
    }

    @Test
    fun vocabularyPreservesEveryStoredFieldAndNestedExample() {
        val vocabulary = mapper.toVocabulary(TrainingContentTestFixtures.getDocument(TrainingContentTestFixtures.getVocabulary()), "vocabulary")
        assertEquals("coffee", vocabulary.word)
        assertEquals("drink", vocabulary.mean)
        assertEquals("/coffee/", vocabulary.phonetic)
        assertEquals("noun", vocabulary.partOfSpeech)
        assertEquals(LearningExample("Example", "Translation"), vocabulary.example)
    }

    @Test
    fun everyRequiredVocabularyFieldRejectsMissingAndBlankContent() {
        listOf("word", "mean", "phonetic", "partOfSpeech").forEach { field ->
            listOf(null, "", " ").forEach { value ->
                val document = TrainingContentTestFixtures.getDocument(TrainingContentTestFixtures.getVocabulary() + (field to value))
                assertThrows(IllegalStateException::class.java) { mapper.toVocabulary(document, "vocabulary") }
            }
        }
    }

    @Test
    fun vocabularyRejectsMalformedNestedExamplesInsteadOfInventingData() {
        listOf(null, "example", mapOf("sentence" to "Example"), mapOf("sentence" to 1, "translate" to "Text")).forEach { example ->
            val document = TrainingContentTestFixtures.getDocument(TrainingContentTestFixtures.getVocabulary() + ("example" to example))
            assertThrows(IllegalStateException::class.java) { mapper.toVocabulary(document, "vocabulary") }
        }
    }

    @Test
    fun grammarAndPatternsPreserveAllExamplesInStoredOrder() {
        val grammar = mapper.toGrammar(TrainingContentTestFixtures.getDocument(TrainingContentTestFixtures.getGrammar()), "grammar")
        val pattern = mapper.toPattern(TrainingContentTestFixtures.getDocument(TrainingContentTestFixtures.getPattern()), "pattern")
        assertEquals("Present simple", grammar.title)
        assertEquals("I work", grammar.structure)
        assertEquals("Routine", grammar.explanation)
        assertEquals("Every day", grammar.usage)
        assertEquals("May I...", pattern.pattern)
        assertEquals("Request", pattern.mean)
        assertEquals("Polite request", pattern.usage)
        assertEquals(listOf(LearningExample("First", "One"), LearningExample("Second", "Two")), grammar.examples)
        assertEquals(grammar.examples, pattern.examples)
    }

    @Test
    fun grammarAndPatternsRejectMissingOrMalformedExampleLists() {
        listOf(null, "not-a-list", listOf("not-a-map"), listOf(mapOf("sentence" to "missing translation"))).forEach { examples ->
            val grammar = TrainingContentTestFixtures.getDocument(TrainingContentTestFixtures.getGrammar() + ("examples" to examples))
            val pattern = TrainingContentTestFixtures.getDocument(TrainingContentTestFixtures.getPattern() + ("examples" to examples))
            assertThrows(IllegalStateException::class.java) { mapper.toGrammar(grammar, "grammar") }
            assertThrows(IllegalStateException::class.java) { mapper.toPattern(pattern, "pattern") }
        }
    }

    @Test
    fun assessmentPreservesRequiredActionIdsDescriptionsAndOrder() {
        val document = TrainingContentTestFixtures.getDocument(mapOf(
            "scenarioId" to "scenario", "title" to "Assessment",
            "requiredActions" to listOf(mapOf("requiredActionId" to "order", "description" to "Order a drink"), mapOf("requiredActionId" to "pay", "description" to "Pay"))
        ))
        val assessment = mapper.toAssessment(document)
        assertEquals("scenario", assessment.scenarioId)
        assertEquals("Assessment", assessment.title)
        assertEquals(listOf(RequiredAction("order", "Order a drink"), RequiredAction("pay", "Pay")), assessment.requiredActions)
    }

    @Test
    fun assessmentRejectsMissingFieldsAndMalformedRequiredActions() {
        val valid = mapOf("scenarioId" to "scenario", "title" to "Assessment", "requiredActions" to emptyList<Any>())
        listOf("scenarioId", "title", "requiredActions").forEach { field ->
            assertThrows(IllegalStateException::class.java) { mapper.toAssessment(TrainingContentTestFixtures.getDocument(valid - field)) }
        }
        listOf("not-a-list", listOf("not-a-map"), listOf(mapOf("description" to "missing id"))).forEach { actions ->
            assertThrows(IllegalStateException::class.java) { mapper.toAssessment(TrainingContentTestFixtures.getDocument(valid + ("requiredActions" to actions))) }
        }
    }
}
