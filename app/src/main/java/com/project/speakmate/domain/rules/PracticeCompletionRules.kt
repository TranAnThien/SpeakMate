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
import java.util.Date

object PracticeCompletionRules {
    private const val MESSAGE_ID_PREFIX = "message_"
    private const val ASSESSMENT_NOT_HISTORY = "Assessment is not practice history"

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
                else -> PracticeType.GOAL_ASSESSMENT
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
            feedbackSummary = feedback
        )
        return PracticeCompletion(attempt, training, normalizedOutcome)
    }
}
