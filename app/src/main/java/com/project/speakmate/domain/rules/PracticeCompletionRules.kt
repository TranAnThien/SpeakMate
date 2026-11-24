package com.project.speakmate.domain.rules

import com.project.speakmate.domain.model.AssessmentOutcome
import com.project.speakmate.domain.model.CallActivityContext
import com.project.speakmate.domain.model.CallActivityType
import com.project.speakmate.domain.model.CallOutcome
import com.project.speakmate.domain.model.CallState
import com.project.speakmate.domain.model.CompletedPractice
import com.project.speakmate.domain.model.InsufficientEvidenceReason
import com.project.speakmate.domain.model.PracticeAttempt
import com.project.speakmate.domain.model.PracticeCompletion
import com.project.speakmate.domain.model.PracticeType
import com.project.speakmate.domain.model.TranscriptMessage
import com.project.speakmate.utils.extension.default
import java.util.Date

object PracticeCompletionRules {
    const val AUDIO_RETENTION_MILLIS = 24 * 60 * 60 * 1_000L

    private const val MESSAGE_ID_PREFIX = "message_"
    private const val AUDIO_UNAVAILABLE_FEEDBACK = "Audio evidence is unavailable or expired."
    private const val UNASSESSED_FEEDBACK = "Session saved without an assessment."
    private const val TECHNICAL_FAILURE_FEEDBACK = "Assessment could not be completed because of a technical error."
    private const val INVALID_EVALUATION_FEEDBACK = "The assessment response could not be validated."
    private const val SPEECH_UNAVAILABLE_FEEDBACK = "There is not enough usable speech evidence for an assessment."
    private const val ASSESSMENT_NOT_HISTORY = "Assessment is not practice history"

    fun canRetryGrading(reason: InsufficientEvidenceReason): Boolean =
        reason == InsufficientEvidenceReason.EVALUATION_FAILED || reason == InsufficientEvidenceReason.EVALUATION_INVALID

    fun isEvidenceExpired(createdAtMillis: Long, nowMillis: Long): Boolean =
        nowMillis < createdAtMillis || nowMillis.toULong() - createdAtMillis.toULong() >= AUDIO_RETENTION_MILLIS.toULong()

    fun canComplete(practice: CompletedPractice): Boolean {
        val session = practice.session
        return practice.context !is CallActivityContext.Assessment &&
            practice.context.scenario.scenarioId.isNotBlank() &&
            session.state == CallState.ENDED && session.outcome == CallOutcome.COMPLETED &&
            session.failure == null && session.hasUserSpeech &&
            session.startedAtEpochMillis != null && session.endedAtEpochMillis != null
    }

    fun getFinalTranscripts(transcript: List<TranscriptMessage>): List<TranscriptMessage> =
        transcript.filter { it.isFinal && it.content.isNotBlank() }.mapIndexed { index, message ->
            message.copy(
                messageId = message.messageId.ifBlank { MESSAGE_ID_PREFIX + index },
                content = message.content.trim(),
                orderIndex = index
            )
        }

    fun createCompletion(
        attemptId: String,
        practice: CompletedPractice,
        outcome: AssessmentOutcome?
    ): PracticeCompletion {
        require(canComplete(practice))
        val training = (practice.context as? CallActivityContext.Training)?.activityScenario
        val normalizedOutcome = if (training == null) null else when (outcome) {
            is AssessmentOutcome.Evaluated -> {
                if (outcome.requiredActionResults.keys != training.requiredActions.map { it.requiredActionId }.toSet()) {
                    AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.EVALUATION_INVALID)
                } else {
                    outcome.copy(result = AssessmentRules.calculateResult(outcome.criteria, training.requiredActions, outcome.requiredActionResults))
                }
            }
            is AssessmentOutcome.InsufficientEvidence -> outcome
            null -> AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.EVALUATION_FAILED)
        }
        val evaluated = normalizedOutcome as? AssessmentOutcome.Evaluated
        val feedback = evaluated?.evaluation?.let {
            listOf(it.range.feedback, it.accuracy.feedback, it.fluency.feedback, it.interaction.feedback, it.coherence.feedback)
                .filter(String::isNotBlank).distinct().joinToString("\n").takeIf(String::isNotBlank)
        }
        val type = when (practice.context) {
            is CallActivityContext.FreePractice -> PracticeType.FREE_PRACTICE
            is CallActivityContext.Training -> when (practice.context.activityType) {
                CallActivityType.TRAINING_UNIT -> PracticeType.TRAINING_UNIT
                CallActivityType.GOAL_ASSESSMENT -> PracticeType.GOAL_ASSESSMENT
                CallActivityType.FREE_PRACTICE, CallActivityType.INITIAL_PLACEMENT,
                CallActivityType.REASSESSMENT -> error(ASSESSMENT_NOT_HISTORY)
            }
            is CallActivityContext.Assessment -> error(ASSESSMENT_NOT_HISTORY)
        }
        val attempt = PracticeAttempt(
            attemptId = attemptId,
            scenarioId = practice.context.scenario.scenarioId,
            practiceType = type,
            startedAt = Date(requireNotNull(practice.session.startedAtEpochMillis)),
            endedAt = Date(requireNotNull(practice.session.endedAtEpochMillis)),
            result = evaluated?.result,
            criteriaResult = evaluated?.criteria,
            requiredActionResults = evaluated?.requiredActionResults.orEmpty().toMap(),
            transcripts = getFinalTranscripts(practice.session.transcript),
            requiredActionDescriptions = training?.requiredActions
                ?.associate { it.requiredActionId to it.description }.orEmpty(),
            feedbackSummary = feedback.default((normalizedOutcome as? AssessmentOutcome.InsufficientEvidence)?.reason?.let {
                when (it) {
                    InsufficientEvidenceReason.AUDIO_UNAVAILABLE -> AUDIO_UNAVAILABLE_FEEDBACK
                    InsufficientEvidenceReason.EVALUATION_FAILED -> TECHNICAL_FAILURE_FEEDBACK
                    InsufficientEvidenceReason.EVALUATION_INVALID -> INVALID_EVALUATION_FEEDBACK
                    InsufficientEvidenceReason.NOT_ENOUGH_SPEECH, InsufficientEvidenceReason.NO_USER_SPEECH,
                    InsufficientEvidenceReason.TRANSCRIPT_UNAVAILABLE -> SPEECH_UNAVAILABLE_FEEDBACK
                    InsufficientEvidenceReason.CALL_FAILED -> UNASSESSED_FEEDBACK
                }
            })
        )
        return PracticeCompletion(attempt, training, normalizedOutcome)
    }
}
