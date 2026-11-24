package com.project.speakmate.data.repository

import com.project.speakmate.data.remote.firebase.FirestoreField
import com.project.speakmate.domain.model.SpeakingLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScenarioPreparationMapperTest {
    @Test(expected = IllegalStateException::class)
    fun missingAiRoleMakesScenarioContentInvalid() {
        ScenarioPreparationMapper.toMetadata(emptyMap())
    }

    @Test(expected = IllegalStateException::class)
    fun blankAiRoleMakesScenarioContentInvalid() {
        ScenarioPreparationMapper.toMetadata(mapOf(FirestoreField.AI_ROLE to "  "))
    }

    @Test
    fun missingOptionalFieldsDoNotCreateSuggestedMetadata() {
        val metadata = ScenarioPreparationMapper.toMetadata(mapOf(FirestoreField.AI_ROLE to "Barista"))
        assertNull(metadata.suggestedLevel)
        assertNull(metadata.estimatedDurationMinutes)
        assertEquals("", metadata.imageUrl)
        assertEquals("", metadata.preCallHint)
    }

    @Test
    fun validMetadataComesOnlyFromTheContentFields() {
        val metadata = ScenarioPreparationMapper.toMetadata(mapOf(FirestoreField.AI_ROLE to "Barista", FirestoreField.SUGGESTED_LEVEL to SpeakingLevel.A2.name,
            FirestoreField.ESTIMATED_DURATION_MINUTES to 5L, FirestoreField.UI_CONTEXT to "  Coffee shop  "))
        assertEquals(SpeakingLevel.A2, metadata.suggestedLevel)
        assertEquals(5, metadata.estimatedDurationMinutes)
        assertEquals("Coffee shop", metadata.uiContext)
    }

    @Test
    fun unknownUnassessedOrWrongTypeLevelIsHidden() {
        listOf("C1", "a2", SpeakingLevel.NOT_ASSESSED.name, 1).forEach {
            assertNull(ScenarioPreparationMapper.toMetadata(mapOf(FirestoreField.AI_ROLE to "Barista", FirestoreField.SUGGESTED_LEVEL to it)).suggestedLevel)
        }
    }

    @Test
    fun invalidDurationIsHiddenInsteadOfTruncatedOrDefaulted() {
        listOf(0, -1, 2.5, Double.NaN, Double.POSITIVE_INFINITY, Long.MAX_VALUE, "5").forEach {
            assertNull(ScenarioPreparationMapper.toMetadata(mapOf(FirestoreField.AI_ROLE to "Barista", FirestoreField.ESTIMATED_DURATION_MINUTES to it)).estimatedDurationMinutes)
        }
    }

    @Test
    fun malformedSupportTextDoesNotMakeAnOtherwiseValidScenarioUnavailable() {
        val metadata = ScenarioPreparationMapper.toMetadata(mapOf(FirestoreField.IMAGE_URL to false,
            FirestoreField.AI_ROLE to "Barista", FirestoreField.PRE_CALL_HINT to 1))
        assertEquals("", metadata.imageUrl)
        assertEquals("Barista", metadata.aiRole)
        assertEquals("", metadata.preCallHint)
    }
}
