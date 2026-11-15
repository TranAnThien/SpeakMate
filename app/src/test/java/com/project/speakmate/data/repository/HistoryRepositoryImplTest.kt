package com.project.speakmate.data.repository

import com.google.firebase.Timestamp
import com.project.speakmate.data.dto.PracticeAttemptDto
import com.project.speakmate.data.dto.TranscriptMessageDto
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
        assertTrue(dto.criteriaResult!!["rangeMet"] == true)
        assertEquals(mapOf("order_coffee" to true), dto.requiredActionResults)
        assertEquals("Great pronunciation!", dto.feedbackSummary)
    }

    @Test
    fun practiceAttemptDtoToDomainRestoresObjectCorrectly() {
        val dto = sampleAttempt.toDto()
        val domain = dto.toDomain(sampleAttempt.transcripts)

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

        val domain = dto.toDomain(emptyList())
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
        assertTrue(dto.isFinal)
        assertTrue(dto.isInterrupted)

        val backToDomain = dto.toDomain()
        assertEquals(msg, backToDomain)
    }
}
