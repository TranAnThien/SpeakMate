package com.project.speakmate.data.repository

import com.google.firebase.firestore.util.CustomClassMapper
import com.project.speakmate.data.dto.LearningExampleDto
import com.project.speakmate.data.dto.PracticeAttemptDto
import com.project.speakmate.data.dto.SpeakingCriteriaResultDto
import com.project.speakmate.data.dto.TrainingProgressDto
import com.project.speakmate.data.dto.UserDto
import com.project.speakmate.data.dto.VocabularyItemDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestoreDtoFieldTest {
    @Test
    fun userWriterPreservesAllDeployedProfileAndProgressKeys() {
        val dto = UserDto(trainingProgress = TrainingProgressDto(1, 2, "PATH_COMPLETED"), latestAssessmentCriteria = SpeakingCriteriaResultDto())
        val stored = toStoredMap(dto)
        assertEquals(setOf("userId", "displayName", "email", "photoUrl", "level", "latestAssessmentCriteria", "lastAssessedAt", "trainingProgress", "savedVocabularies"), stored.keys)
        val progress = stored["trainingProgress"] as Map<*, *>
        assertEquals(setOf("currentGoalOrder", "currentUnitOrder", "stage"), progress.keys)
        assertEquals("PATH_COMPLETED", progress["stage"])
        assertEquals(1L, (progress["currentGoalOrder"] as Number).toLong())
        assertEquals(2L, (progress["currentUnitOrder"] as Number).toLong())
    }

    @Test
    fun vocabularyAndExampleUseOnlyTheDeployedNestedKeys() {
        val dto = VocabularyItemDto("coffee", "drink", "", "noun", LearningExampleDto("A coffee", "Translation"))
        val stored = toStoredMap(dto)
        assertEquals(setOf("word", "mean", "phonetic", "partOfSpeech", "example"), stored.keys)
        val example = stored["example"] as Map<*, *>
        assertEquals(setOf("sentence", "translate"), example.keys)
        assertEquals("Translation", example["translate"])
        assertEquals(dto, CustomClassMapper.convertToCustomClass(stored, VocabularyItemDto::class.java, null))
    }

    @Test
    fun attemptWriterHasNoEmbeddedTranscriptOrDerivedDomainProperties() {
        val stored = toStoredMap(PracticeAttemptDto())
        assertEquals(setOf("attemptId", "scenarioId", "practiceType", "startedAt", "endedAt", "result", "criteriaResult", "requiredActionResults", "feedbackSummary"), stored.keys)
        assertFalse(stored.containsKey("transcripts"))
        assertFalse(stored.containsKey("durationSeconds"))
    }

    @Test
    fun criteriaWriterKeepsFalseValuesAndRoundTripsTheExactFiveFields() {
        val dto = SpeakingCriteriaResultDto()
        val stored = toStoredMap(dto)
        assertEquals(setOf("rangeMet", "accuracyMet", "fluencyMet", "interactionMet", "coherenceMet"), stored.keys)
        assertTrue(stored.values.all { it == false })
        assertEquals(dto, CustomClassMapper.convertToCustomClass(stored, SpeakingCriteriaResultDto::class.java, null))
    }

    private fun toStoredMap(value: Any): Map<*, *> = CustomClassMapper.convertToPlainJavaTypes(value) as Map<*, *>
}
