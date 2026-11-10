package com.project.speakmate.domain.rules

import com.project.speakmate.domain.model.AssessmentEvidence
import com.project.speakmate.domain.model.CallOutcome
import com.project.speakmate.domain.model.CriterionJudgement
import com.project.speakmate.domain.model.SenderType
import com.project.speakmate.domain.model.SpeakingEvaluation
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TranscriptMessage

object AssessmentTestFixtures {
    const val USER_QUOTE = "I'd like a latte, please."
    const val AI_QUOTE = "We also have fresh orange juice."
    const val REQUIRED_ACTION_ID = "RA1_ORDER_DRINK"
    const val AUDIO_DURATION_MILLIS = 10_000L

    fun getPassEvidenceSet(): Pair<AssessmentEvidence, SpeakingEvaluation> = getEvidence() to getEvaluation()

    fun getPartialEvidenceSet(): Pair<AssessmentEvidence, SpeakingEvaluation> = getEvidence() to getEvaluation(false, false, true, true, true)

    fun getFailEvidenceSet(): Pair<AssessmentEvidence, SpeakingEvaluation> = getEvidence() to getEvaluation(false, false, false, false, true)

    fun getInsufficientEvidenceSet(): Pair<AssessmentEvidence, SpeakingEvaluation> =
        getEvidence().copy(audioDurationMillis = null) to getEvaluation()

    fun getEvidence(): AssessmentEvidence = AssessmentEvidence(
        targetLevel = SpeakingLevel.A2,
        communicativeGoal = "Order a drink",
        requiredActions = emptyList(),
        callOutcome = CallOutcome.COMPLETED,
        failure = null,
        hasUserSpeech = true,
        transcript = listOf(
            getMessage(USER_QUOTE),
            getMessage("What size would you like?", SenderType.AI, orderIndex = 1),
            getMessage("A medium, please.", orderIndex = 2),
            getMessage("Thank you, that is all.", orderIndex = 3),
            getMessage(AI_QUOTE, SenderType.AI, orderIndex = 4)
        ),
        audioDurationMillis = AUDIO_DURATION_MILLIS
    )

    fun getMessage(
        content: String,
        senderType: SenderType = SenderType.USER,
        isFinal: Boolean = true,
        orderIndex: Int = 0
    ): TranscriptMessage = TranscriptMessage(
        messageId = "message_$orderIndex",
        senderType = senderType,
        content = content,
        orderIndex = orderIndex,
        isFinal = isFinal,
        isInterrupted = false
    )

    fun getEvaluation(
        isRangeMet: Boolean = true,
        isAccuracyMet: Boolean = true,
        isFluencyMet: Boolean = true,
        isInteractionMet: Boolean = true,
        isCoherenceMet: Boolean = true
    ): SpeakingEvaluation = SpeakingEvaluation(
        range = getJudgement(isRangeMet),
        accuracy = getJudgement(isAccuracyMet),
        fluency = getJudgement(isFluencyMet).copy(audioStartMillis = 0L, audioEndMillis = AUDIO_DURATION_MILLIS),
        interaction = getJudgement(isInteractionMet),
        coherence = getJudgement(isCoherenceMet),
        requiredActions = emptyMap()
    )

    private fun getJudgement(isMet: Boolean): CriterionJudgement = CriterionJudgement(
        isMet = isMet,
        userQuote = USER_QUOTE,
        feedback = "Assessment feedback",
        audioStartMillis = null,
        audioEndMillis = null
    )
}
