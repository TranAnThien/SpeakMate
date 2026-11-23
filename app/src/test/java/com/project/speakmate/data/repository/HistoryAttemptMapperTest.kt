package com.project.speakmate.data.repository

import com.google.firebase.Timestamp
import com.project.speakmate.data.dto.PracticeAttemptDto
import com.project.speakmate.domain.model.HistoryReadException
import com.project.speakmate.domain.model.HistoryReadFailure
import com.project.speakmate.domain.model.durationInSeconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class HistoryAttemptMapperTest {
    private val dto = PracticeAttemptDto(attemptId = "attempt", scenarioId = "scenario", practiceType = "FREE_PRACTICE",
        startedAt = Timestamp(1, 0), endedAt = Timestamp(2, 0))

    @Test
    fun missingAttemptRemainsNotFoundInsteadOfAnEmptySuccessfulSession() {
        val failure = assertThrows(HistoryReadException::class.java) { HistoryAttemptMapper.toAttempt("attempt", null, null) }
        assertEquals(HistoryReadFailure.NOT_FOUND, failure.reason)
    }

    @Test
    fun invalidIdentityDatesAndStoredEnumsAreRejected() {
        listOf(dto.copy(attemptId = "other"), dto.copy(startedAt = null), dto.copy(endedAt = null),
            dto.copy(practiceType = "unknown"), dto.copy(result = "unknown")).forEach { invalid ->
            assertEquals(HistoryReadFailure.INVALID_DATA, assertThrows(HistoryReadException::class.java) {
                HistoryAttemptMapper.toAttempt("attempt", "attempt", invalid)
            }.reason)
        }
    }

    @Test
    fun resultLoadsWithoutDependingOnTranscriptAvailability() {
        val attempt = HistoryAttemptMapper.toAttempt("attempt", "attempt", dto)
        assertEquals("attempt", attempt.attemptId)
        assertEquals(null, attempt.result)
        assertTrue(attempt.transcripts.isEmpty())
    }
    @Test
    fun clockAdjustmentPreservesRecordedTimestampsAndExistingDurationPolicy() {
        val attempt = HistoryAttemptMapper.toAttempt("attempt", "attempt", dto.copy(endedAt = Timestamp(0, 0)))
        assertEquals(1_000L, attempt.startedAt.time)
        assertEquals(0L, attempt.endedAt.time)
        assertEquals(0L, attempt.durationInSeconds)
    }

}
