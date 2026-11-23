package com.project.speakmate.data.repository

import com.project.speakmate.data.dto.PracticeAttemptDto
import com.project.speakmate.data.dto.toDomain
import com.project.speakmate.domain.model.HistoryReadException
import com.project.speakmate.domain.model.HistoryReadFailure
import com.project.speakmate.domain.model.PracticeAttempt

object HistoryAttemptMapper {
    fun toAttempt(requestedId: String, documentId: String?, dto: PracticeAttemptDto?): PracticeAttempt {
        if (documentId == null) throw HistoryReadException(HistoryReadFailure.NOT_FOUND)
        if (dto == null || documentId != requestedId || dto.attemptId != documentId || dto.scenarioId.isBlank() ||
            dto.startedAt == null || dto.endedAt == null) throw HistoryReadException(HistoryReadFailure.INVALID_DATA)
        return dto.toDomain(emptyList()) ?: throw HistoryReadException(HistoryReadFailure.INVALID_DATA)
    }
}
