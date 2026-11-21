package com.project.speakmate.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.util.CustomClassMapper
import com.project.speakmate.data.dto.toDomain
import com.project.speakmate.data.dto.toDto
import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.PracticeAttempt
import com.project.speakmate.domain.model.PracticeType
import com.project.speakmate.domain.model.SenderType
import com.project.speakmate.domain.model.SpeakingCriteriaResult
import com.project.speakmate.domain.model.TranscriptMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class HistoryRepositoryImplTest {

    private val sampleAttempt = PracticeAttempt(
        attemptId = "attempt_100",
        scenarioId = "cafe_scenario",
        practiceType = PracticeType.TRAINING_UNIT,
        startedAt = Date(1700000000000L),
        endedAt = Date(1700000060000L),
        result = AssessmentResult.PASS,
        criteriaResult = SpeakingCriteriaResult(
            rangeMet = true,
            accuracyMet = true,
            fluencyMet = true,
            interactionMet = true,
            coherenceMet = true
        ),
        requiredActionResults = mapOf("order_coffee" to true),
        transcripts = listOf(
            TranscriptMessage("m1", SenderType.USER, "One latte please", 0, isFinal = true, isInterrupted = false),
            TranscriptMessage("m2", SenderType.AI, "Hot or cold?", 1, isFinal = true, isInterrupted = false)
        ),
        feedbackSummary = "Great pronunciation!"
    )

    @Test
    fun practiceAttemptToDtoMapsAllFieldsCorrectly() {
        val dto = sampleAttempt.toDto()

        assertEquals("attempt_100", dto.attemptId)
        assertEquals("cafe_scenario", dto.scenarioId)
        assertEquals(PracticeType.TRAINING_UNIT.name, dto.practiceType)
        assertEquals(Timestamp(sampleAttempt.startedAt), dto.startedAt)
        assertEquals(Timestamp(sampleAttempt.endedAt), dto.endedAt)
        assertEquals(AssessmentResult.PASS.name, dto.result)
        assertNotNull(dto.criteriaResult)
        assertEquals(true, dto.criteriaResult?.rangeMet)
        assertEquals(mapOf("order_coffee" to true), dto.requiredActionResults)
        assertEquals("Great pronunciation!", dto.feedbackSummary)
    }

    @Test
    fun practiceAttemptDtoToDomainRestoresObjectCorrectly() {
        val dto = sampleAttempt.toDto()
        val domain = requireNotNull(dto.toDomain(sampleAttempt.transcripts))

        assertEquals(sampleAttempt.attemptId, domain.attemptId)
        assertEquals(sampleAttempt.scenarioId, domain.scenarioId)
        assertEquals(sampleAttempt.practiceType, domain.practiceType)
        assertEquals(sampleAttempt.startedAt, domain.startedAt)
        assertEquals(sampleAttempt.endedAt, domain.endedAt)
        assertEquals(sampleAttempt.result, domain.result)
        assertEquals(sampleAttempt.criteriaResult, domain.criteriaResult)
        assertEquals(sampleAttempt.requiredActionResults, domain.requiredActionResults)
        assertEquals(2, domain.transcripts.size)
        assertEquals("Great pronunciation!", domain.feedbackSummary)
    }

    @Test
    fun nullResultAttemptToDtoAndToDomainPreservesNullValues() {
        val nullAttempt = sampleAttempt.copy(
            result = null,
            criteriaResult = null,
            feedbackSummary = "Thiếu bằng chứng"
        )
        val dto = nullAttempt.toDto()
        assertNull(dto.result)
        assertNull(dto.criteriaResult)

        val domain = requireNotNull(dto.toDomain(emptyList()))
        assertNull(domain.result)
        assertNull(domain.criteriaResult)
        assertEquals("Thiếu bằng chứng", domain.feedbackSummary)
    }

    @Test
    fun transcriptMessageDtoMappingPreservesAllFields() {
        val msg = TranscriptMessage("msg_1", SenderType.AI, "Welcome!", 0, isFinal = true, isInterrupted = true)
        val dto = msg.toDto()

        assertEquals("msg_1", dto.messageId)
        assertEquals(SenderType.AI.name, dto.senderType)
        assertEquals("Welcome!", dto.content)
        assertEquals(0, dto.orderIndex)
        assertEquals(true, dto.isFinal)
        assertEquals(true, dto.isInterrupted)

        val backToDomain = dto.toDomain()
        assertEquals(msg, backToDomain)
    }

    @Test
    fun attemptMappingRejectsUnknownPracticeType() {
        assertNull(sampleAttempt.toDto().copy(practiceType = "UNKNOWN_TYPE").toDomain(emptyList()))
    }

    @Test
    fun attemptMappingRejectsUnknownAssessmentResult() {
        assertNull(sampleAttempt.toDto().copy(result = "UNKNOWN_RESULT").toDomain(emptyList()))
    }

    @Test
    fun transcriptMappingRejectsUnknownSenderType() {
        val dto = sampleAttempt.transcripts.first().toDto().copy(senderType = "UNKNOWN_SENDER")
        assertNull(dto.toDomain())
    }

    @Test
    fun transcriptMappingUsesBothLegacyFlagsWhenCanonicalFlagsAreMissing() {
        val dto = sampleAttempt.transcripts.first().toDto().copy(isFinal = null, isInterrupted = null)

        val message = requireNotNull(dto.toDomain(legacyFinal = false, legacyInterrupted = true))

        assertEquals(false, message.isFinal)
        assertEquals(true, message.isInterrupted)
    }

    @Test
    fun transcriptMappingKeepsCanonicalFalseFlagsWhenLegacyFlagsAreTrue() {
        val dto = sampleAttempt.transcripts.first().toDto().copy(isFinal = false, isInterrupted = false)

        val message = requireNotNull(dto.toDomain(legacyFinal = true, legacyInterrupted = true))

        assertEquals(false, message.isFinal)
        assertEquals(false, message.isInterrupted)
    }

    @Test
    fun firestoreMapperWritesAndReadsCanonicalTranscriptFlagsOnly() {
        val message = sampleAttempt.transcripts.first().copy(isFinal = false, isInterrupted = true)
        val persisted = CustomClassMapper.convertToPlainJavaTypes(message.toDto()) as Map<*, *>

        assertEquals(false, persisted["isFinal"])
        assertEquals(true, persisted["isInterrupted"])
        assertTrue("final" !in persisted)
        assertTrue("interrupted" !in persisted)

        val dto = CustomClassMapper.convertToCustomClass(persisted, message.toDto()::class.java, null)
        assertEquals(message, dto.toDomain())
    }

    @Test
    fun firestoreMapperPreservesTheNestedCriteriaFieldNames() {
        val persisted = CustomClassMapper.convertToPlainJavaTypes(sampleAttempt.toDto()) as Map<*, *>
        val criteria = persisted["criteriaResult"] as Map<*, *>

        assertEquals(setOf("rangeMet", "accuracyMet", "fluencyMet", "interactionMet", "coherenceMet"), criteria.keys)
    }
}
