package com.project.speakmate.data.dto

import com.google.firebase.Timestamp
import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.PracticeAttempt
import com.project.speakmate.domain.model.PracticeType
import com.project.speakmate.domain.model.SenderType
import com.project.speakmate.domain.model.SpeakingCriteriaResult
import com.project.speakmate.domain.model.TranscriptMessage
import java.util.Date

data class PracticeAttemptDto(
    val attemptId: String = "",
    val scenarioId: String = "",
    val practiceType: String = "",
    val startedAt: Timestamp? = null,
    val endedAt: Timestamp? = null,
    val result: String? = null,
    val criteriaResult: Map<String, Boolean>? = null,
    val requiredActionResults: Map<String, Boolean> = emptyMap(),
    val feedbackSummary: String? = null
)

data class TranscriptMessageDto(
    val messageId: String = "",
    val senderType: String = "",
    val content: String = "",
    val orderIndex: Int = 0,
    val isFinal: Boolean = true,
    val isInterrupted: Boolean = false
)

fun PracticeAttempt.toDto(): PracticeAttemptDto {
    return PracticeAttemptDto(
        attemptId = this.attemptId,
        scenarioId = this.scenarioId,
        practiceType = this.practiceType.name,
        startedAt = Timestamp(this.startedAt),
        endedAt = Timestamp(this.endedAt),
        result = this.result?.name,
        criteriaResult = this.criteriaResult?.let {
            mapOf(
                "rangeMet" to it.rangeMet,
                "accuracyMet" to it.accuracyMet,
                "fluencyMet" to it.fluencyMet,
                "interactionMet" to it.interactionMet,
                "coherenceMet" to it.coherenceMet
            )
        },
        requiredActionResults = this.requiredActionResults,
        feedbackSummary = this.feedbackSummary
    )
}

fun PracticeAttemptDto.toDomain(transcripts: List<TranscriptMessage>): PracticeAttempt? {
    val type = PracticeType.entries.firstOrNull { it.name == practiceType } ?: return null
    val assessment = result?.let { storedResult ->
        AssessmentResult.entries.firstOrNull { it.name == storedResult } ?: return null
    }
    val started = this.startedAt?.toDate() ?: Date()
    val ended = this.endedAt?.toDate() ?: started
    return PracticeAttempt(
        attemptId = this.attemptId,
        scenarioId = this.scenarioId,
        practiceType = type,
        startedAt = started,
        endedAt = ended,
        result = assessment,
        criteriaResult = this.criteriaResult?.let {
            SpeakingCriteriaResult(
                rangeMet = it["rangeMet"] ?: false,
                accuracyMet = it["accuracyMet"] ?: false,
                fluencyMet = it["fluencyMet"] ?: false,
                interactionMet = it["interactionMet"] ?: false,
                coherenceMet = it["coherenceMet"] ?: false
            )
        },
        requiredActionResults = this.requiredActionResults,
        transcripts = transcripts,
        feedbackSummary = this.feedbackSummary
    )
}

fun TranscriptMessage.toDto(): TranscriptMessageDto {
    return TranscriptMessageDto(
        messageId = this.messageId,
        senderType = this.senderType.name,
        content = this.content,
        orderIndex = this.orderIndex,
        isFinal = this.isFinal,
        isInterrupted = this.isInterrupted
    )
}

fun TranscriptMessageDto.toDomain(): TranscriptMessage? {
    val sender = SenderType.entries.firstOrNull { it.name == senderType } ?: return null
    return TranscriptMessage(
        messageId = this.messageId,
        senderType = sender,
        content = this.content,
        orderIndex = this.orderIndex,
        isFinal = this.isFinal,
        isInterrupted = this.isInterrupted
    )
}
