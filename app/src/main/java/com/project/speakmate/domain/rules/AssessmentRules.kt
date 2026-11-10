package com.project.speakmate.domain.rules

import com.project.speakmate.domain.model.AssessmentEvidence
import com.project.speakmate.domain.model.AssessmentOutcome
import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.CallOutcome
import com.project.speakmate.domain.model.InsufficientEvidenceReason
import com.project.speakmate.domain.model.RequiredAction
import com.project.speakmate.domain.model.SenderType
import com.project.speakmate.domain.model.SpeakingCriteriaResult
import com.project.speakmate.domain.model.SpeakingEvaluation
import java.util.Locale

object AssessmentRules {
    private const val MIN_USER_WORDS = 20
    private const val MIN_USER_TURNS = 3
    private val WHITESPACE = Regex("\\s+")
    private val PUNCTUATION = Regex("\\p{P}+")

    fun calculateResult(
        criteria: SpeakingCriteriaResult,
        requiredActions: List<RequiredAction>,
        requiredActionResults: Map<String, Boolean>
    ): AssessmentResult {
        if (requiredActions.any { requiredActionResults[it.requiredActionId] != true }) return AssessmentResult.FAIL
        return criteria.calculateSpeakingResult()
    }

    fun findEvidenceProblem(evidence: AssessmentEvidence): InsufficientEvidenceReason? {
        if (evidence.callOutcome == CallOutcome.FAILED || evidence.failure != null) {
            return InsufficientEvidenceReason.CALL_FAILED
        }
        if (evidence.callOutcome == CallOutcome.CANCELLED || !evidence.hasUserSpeech) {
            return InsufficientEvidenceReason.NO_USER_SPEECH
        }
        val userTurns = getFinalUserTurns(evidence)
        if (userTurns.isEmpty()) return InsufficientEvidenceReason.TRANSCRIPT_UNAVAILABLE
        val wordCount = userTurns.sumOf { normalize(it).split(WHITESPACE).count(String::isNotBlank) }
        if (userTurns.size < MIN_USER_TURNS && wordCount < MIN_USER_WORDS) {
            return InsufficientEvidenceReason.NOT_ENOUGH_SPEECH
        }
        val durationMillis = evidence.audioDurationMillis
        if (durationMillis == null || durationMillis <= 0L) return InsufficientEvidenceReason.AUDIO_UNAVAILABLE
        return null
    }

    fun findEvaluationProblem(
        evidence: AssessmentEvidence,
        evaluation: SpeakingEvaluation
    ): InsufficientEvidenceReason? {
        val userTurns = getFinalUserTurns(evidence).map(::normalize)
        val transcriptCriteria = listOf(evaluation.range, evaluation.accuracy, evaluation.interaction, evaluation.coherence)
        if (transcriptCriteria.any { !hasUserQuote(it.userQuote, userTurns) }) {
            return InsufficientEvidenceReason.EVALUATION_INVALID
        }
        val startMillis = evaluation.fluency.audioStartMillis
        val endMillis = evaluation.fluency.audioEndMillis
        val durationMillis = evidence.audioDurationMillis
        if (durationMillis == null || startMillis == null || endMillis == null ||
            startMillis < 0L || startMillis >= endMillis || endMillis > durationMillis
        ) return InsufficientEvidenceReason.EVALUATION_INVALID
        if (evaluation.requiredActions.keys != evidence.requiredActions.map { it.requiredActionId }.toSet()) {
            return InsufficientEvidenceReason.EVALUATION_INVALID
        }
        if (evaluation.requiredActions.values.any { it.isCompleted && !hasUserQuote(it.userQuote, userTurns) }) {
            return InsufficientEvidenceReason.EVALUATION_INVALID
        }
        return null
    }

    fun assess(evidence: AssessmentEvidence, evaluation: SpeakingEvaluation): AssessmentOutcome {
        val evidenceProblem = findEvidenceProblem(evidence)
        if (evidenceProblem != null) return AssessmentOutcome.InsufficientEvidence(evidenceProblem)
        val evaluationProblem = findEvaluationProblem(evidence, evaluation)
        if (evaluationProblem != null) return AssessmentOutcome.InsufficientEvidence(evaluationProblem)
        val criteria = SpeakingCriteriaResult(
            rangeMet = evaluation.range.isMet,
            accuracyMet = evaluation.accuracy.isMet,
            fluencyMet = evaluation.fluency.isMet,
            interactionMet = evaluation.interaction.isMet,
            coherenceMet = evaluation.coherence.isMet
        )
        val requiredActionResults = evaluation.requiredActions.mapValues { it.value.isCompleted }
        return AssessmentOutcome.Evaluated(
            result = calculateResult(criteria, evidence.requiredActions, requiredActionResults),
            criteria = criteria,
            requiredActionResults = requiredActionResults,
            evaluation = evaluation
        )
    }

    private fun getFinalUserTurns(evidence: AssessmentEvidence): List<String> = evidence.transcript
        .filter { it.senderType == SenderType.USER && it.isFinal && it.content.isNotBlank() }
        .map { it.content }

    private fun hasUserQuote(quote: String, userTurns: List<String>): Boolean {
        val normalizedQuote = normalize(quote)
        return normalizedQuote.isNotEmpty() && userTurns.any { " $normalizedQuote " in " $it " }
    }

    private fun normalize(text: String): String =
        text.lowercase(Locale.ROOT).replace(PUNCTUATION, "").trim().replace(WHITESPACE, " ")
}

