package com.project.speakmate.domain.rules

import com.project.speakmate.domain.model.AssessmentEvidence
import com.project.speakmate.domain.model.AssessmentOutcome
import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.CallFailure
import com.project.speakmate.domain.model.CallOutcome
import com.project.speakmate.domain.model.InsufficientEvidenceReason
import com.project.speakmate.domain.model.RequiredAction
import com.project.speakmate.domain.model.RequiredActionJudgement
import com.project.speakmate.domain.model.SenderType
import com.project.speakmate.domain.model.SpeakingCriteriaResult
import com.project.speakmate.domain.model.SpeakingEvaluation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AssessmentRulesTest {
    private val evidence = AssessmentTestFixtures.getEvidence()
    private val evaluation = AssessmentTestFixtures.getEvaluation()
    private val requiredAction = RequiredAction(AssessmentTestFixtures.REQUIRED_ACTION_ID, "Order a drink")

    @Test
    fun fiveCriteriaMetIsPass() = assertThreshold(AssessmentResult.PASS, true, true, true, true, true)

    @Test
    fun fourCriteriaMetIsPass() = assertThreshold(AssessmentResult.PASS, false, true, true, true, true)

    @Test
    fun threeCriteriaMetIsPartial() = assertThreshold(AssessmentResult.PARTIAL, false, false, true, true, true)

    @Test
    fun twoCriteriaMetIsFail() = assertThreshold(AssessmentResult.FAIL, false, false, false, true, true)

    @Test
    fun noCriteriaMetIsFail() = assertThreshold(AssessmentResult.FAIL, false, false, false, false, false)

    @Test
    fun incompleteRequiredActionFailsEvenWithAllCriteriaMet() {
        val outcome = AssessmentRules.assess(
            evidence.copy(requiredActions = listOf(requiredAction)),
            evaluation.copy(requiredActions = mapOf(requiredAction.requiredActionId to RequiredActionJudgement(false, "")))
        )
        assertEquals(AssessmentResult.FAIL, (outcome as AssessmentOutcome.Evaluated).result)
    }

    @Test
    fun allRequiredActionsCompletedUsesThreshold() {
        val outcome = AssessmentRules.assess(
            evidence.copy(requiredActions = listOf(requiredAction)),
            evaluation.copy(requiredActions = mapOf(requiredAction.requiredActionId to
                RequiredActionJudgement(true, AssessmentTestFixtures.USER_QUOTE)))
        )
        assertEquals(AssessmentResult.PASS, (outcome as AssessmentOutcome.Evaluated).result)
    }

    @Test
    fun noRequiredActionsSkipsGate() {
        assertEquals(AssessmentResult.PASS, (AssessmentRules.assess(evidence, evaluation) as AssessmentOutcome.Evaluated).result)
    }

    @Test
    fun missingRequiredActionKeyIsInvalidEvaluation() {
        assertInvalid(evidence.copy(requiredActions = listOf(requiredAction)), evaluation)
    }

    @Test
    fun unexpectedRequiredActionKeyIsInvalidEvaluation() {
        assertInvalid(evidence, evaluation.copy(requiredActions = mapOf(requiredAction.requiredActionId to
            RequiredActionJudgement(false, ""))))
    }

    @Test
    fun completedActionWithoutUserQuoteIsInvalidEvaluation() {
        assertInvalid(evidence.copy(requiredActions = listOf(requiredAction)), evaluation.copy(
            requiredActions = mapOf(requiredAction.requiredActionId to RequiredActionJudgement(true, ""))
        ))
    }

    @Test
    fun completedActionWithAiQuoteIsInvalidEvaluation() {
        assertInvalid(evidence.copy(requiredActions = listOf(requiredAction)), evaluation.copy(
            requiredActions = mapOf(requiredAction.requiredActionId to
                RequiredActionJudgement(true, AssessmentTestFixtures.AI_QUOTE))
        ))
    }

    @Test
    fun failedCallIsInsufficientEvidence() {
        assertProblem(evidence.copy(callOutcome = CallOutcome.FAILED), InsufficientEvidenceReason.CALL_FAILED)
    }

    @Test
    fun callFailureIsInsufficientEvenWhenCompleted() {
        assertProblem(evidence.copy(failure = CallFailure.NETWORK), InsufficientEvidenceReason.CALL_FAILED)
    }

    @Test
    fun cancelledCallHasNoUserSpeech() {
        assertProblem(evidence.copy(callOutcome = CallOutcome.CANCELLED), InsufficientEvidenceReason.NO_USER_SPEECH)
    }

    @Test
    fun missingSpeechFlagHasNoUserSpeech() {
        assertProblem(evidence.copy(hasUserSpeech = false), InsufficientEvidenceReason.NO_USER_SPEECH)
    }

    @Test
    fun missingFinalUserTranscriptIsInsufficient() {
        assertProblem(evidence.copy(transcript = emptyList()), InsufficientEvidenceReason.TRANSCRIPT_UNAVAILABLE)
    }

    @Test
    fun partialTranscriptIsIgnored() {
        assertProblem(evidence.copy(transcript = evidence.transcript.map { it.copy(isFinal = false) }),
            InsufficientEvidenceReason.TRANSCRIPT_UNAVAILABLE)
    }

    @Test
    fun tooLittleSpeechIsInsufficient() {
        assertProblem(evidence.copy(transcript = listOf(AssessmentTestFixtures.getMessage("Hello"))),
            InsufficientEvidenceReason.NOT_ENOUGH_SPEECH)
    }

    @Test
    fun oneShortTurnDoesNotFailWholeActivity() {
        assertNull(AssessmentRules.findEvidenceProblem(evidence))
    }

    @Test
    fun twentyWordsInOneTurnAreEnough() {
        val transcript = listOf(AssessmentTestFixtures.getMessage(
            "I would like to buy a medium coffee with some milk and sugar because I have a long journey today"
        ))
        assertNull(AssessmentRules.findEvidenceProblem(evidence.copy(transcript = transcript)))
    }

    @Test
    fun nineteenWordsInTwoTurnsAreNotEnough() {
        val transcript = listOf(
            AssessmentTestFixtures.getMessage("I would like to buy a medium coffee with some milk"),
            AssessmentTestFixtures.getMessage("and sugar because I have a long journey", orderIndex = 1)
        )
        assertProblem(evidence.copy(transcript = transcript), InsufficientEvidenceReason.NOT_ENOUGH_SPEECH)
    }

    @Test
    fun aiAndPartialWordsDoNotSatisfyMinimum() {
        val transcript = listOf(
            AssessmentTestFixtures.getMessage("Hello"),
            AssessmentTestFixtures.getMessage("word ".repeat(20), SenderType.AI, orderIndex = 1),
            AssessmentTestFixtures.getMessage("word ".repeat(20), isFinal = false, orderIndex = 2)
        )
        assertProblem(evidence.copy(transcript = transcript), InsufficientEvidenceReason.NOT_ENOUGH_SPEECH)
    }

    @Test
    fun missingAudioIsInsufficient() {
        assertProblem(evidence.copy(audioDurationMillis = null), InsufficientEvidenceReason.AUDIO_UNAVAILABLE)
    }

    @Test
    fun zeroAndNegativeAudioAreInsufficient() {
        listOf(0L, -1L).forEach { assertProblem(evidence.copy(audioDurationMillis = it), InsufficientEvidenceReason.AUDIO_UNAVAILABLE) }
    }

    @Test
    fun evidenceProblemsFollowSpecifiedPriority() {
        var invalid = evidence.copy(callOutcome = CallOutcome.FAILED, hasUserSpeech = false, transcript = emptyList(), audioDurationMillis = null)
        assertProblem(invalid, InsufficientEvidenceReason.CALL_FAILED)
        invalid = invalid.copy(callOutcome = CallOutcome.COMPLETED)
        assertProblem(invalid, InsufficientEvidenceReason.NO_USER_SPEECH)
        invalid = invalid.copy(hasUserSpeech = true)
        assertProblem(invalid, InsufficientEvidenceReason.TRANSCRIPT_UNAVAILABLE)
        assertProblem(invalid.copy(transcript = listOf(AssessmentTestFixtures.getMessage("Hello"))), InsufficientEvidenceReason.NOT_ENOUGH_SPEECH)
    }

    @Test
    fun quoteNotSpokenByUserIsInvalid() {
        assertInvalid(evidence, evaluation.copy(range = evaluation.range.copy(userQuote = "I work in a hospital")))
    }

    @Test
    fun quoteFromAiTurnIsInvalid() {
        assertInvalid(evidence, evaluation.copy(accuracy = evaluation.accuracy.copy(userQuote = AssessmentTestFixtures.AI_QUOTE)))
    }

    @Test
    fun quoteDoesNotMatchPartOfAWord() {
        val transcript = listOf(
            AssessmentTestFixtures.getMessage("cat"),
            AssessmentTestFixtures.getMessage("medium please", orderIndex = 1),
            AssessmentTestFixtures.getMessage("thank you", orderIndex = 2)
        )
        assertInvalid(evidence.copy(transcript = transcript), evaluation.copy(
            range = evaluation.range.copy(userQuote = "a"),
            accuracy = evaluation.accuracy.copy(userQuote = "cat"),
            interaction = evaluation.interaction.copy(userQuote = "cat"),
            coherence = evaluation.coherence.copy(userQuote = "cat")
        ))
    }

    @Test
    fun consecutiveWordsInOneUserTurnMatchQuote() {
        assertNull(AssessmentRules.findEvaluationProblem(evidence, evaluation.copy(range = evaluation.range.copy(userQuote = "a latte please"))))
    }

    @Test
    fun quoteWithCorrectWordsInWrongOrderIsInvalid() {
        assertInvalid(evidence, evaluation.copy(range = evaluation.range.copy(userQuote = "latte a please")))
    }

    @Test
    fun quoteCannotSpanTwoUserTurns() {
        assertInvalid(evidence, evaluation.copy(range = evaluation.range.copy(userQuote = "please a medium")))
    }

    @Test
    fun quoteMatchesIgnoringCaseAndPunctuation() {
        val normalized = evaluation.range.copy(userQuote = "  I'D   LIKE A LATTE PLEASE!!!  ")
        assertNull(AssessmentRules.findEvaluationProblem(evidence, evaluation.copy(range = normalized)))
    }

    @Test
    fun quoteFromPartialUserTurnIsInvalid() {
        val transcript = evidence.transcript + AssessmentTestFixtures.getMessage("My unfinished statement", isFinal = false, orderIndex = 5)
        assertInvalid(evidence.copy(transcript = transcript), evaluation.copy(coherence = evaluation.coherence.copy(userQuote = "unfinished statement")))
    }

    @Test
    fun emptyOrPunctuationOnlyQuoteIsInvalid() {
        listOf("", "   ", "...!").forEach {
            assertInvalid(evidence, evaluation.copy(interaction = evaluation.interaction.copy(userQuote = it)))
        }
    }

    @Test
    fun fluencyIntervalOutsideAudioIsInvalid() {
        assertInvalid(evidence, evaluation.copy(fluency = evaluation.fluency.copy(audioEndMillis = AssessmentTestFixtures.AUDIO_DURATION_MILLIS + 1)))
        assertInvalid(evidence, evaluation.copy(fluency = evaluation.fluency.copy(audioStartMillis = -1L)))
    }

    @Test
    fun invertedFluencyIntervalIsInvalid() {
        assertInvalid(evidence, evaluation.copy(fluency = evaluation.fluency.copy(audioStartMillis = AssessmentTestFixtures.AUDIO_DURATION_MILLIS)))
        assertInvalid(evidence, evaluation.copy(fluency = evaluation.fluency.copy(audioStartMillis = AssessmentTestFixtures.AUDIO_DURATION_MILLIS + 1)))
    }

    @Test
    fun missingFluencyIntervalIsInvalid() {
        assertInvalid(evidence, evaluation.copy(fluency = evaluation.fluency.copy(audioStartMillis = null)))
        assertInvalid(evidence, evaluation.copy(fluency = evaluation.fluency.copy(audioEndMillis = null)))
    }

    @Test
    fun passEvidenceSetIsPass() = assertEvidenceSet(AssessmentTestFixtures.getPassEvidenceSet(), AssessmentResult.PASS)

    @Test
    fun partialEvidenceSetIsPartial() = assertEvidenceSet(AssessmentTestFixtures.getPartialEvidenceSet(), AssessmentResult.PARTIAL)

    @Test
    fun failEvidenceSetIsFail() = assertEvidenceSet(AssessmentTestFixtures.getFailEvidenceSet(), AssessmentResult.FAIL)

    @Test
    fun insufficientEvidenceSetNeverProducesResult() {
        val sample = AssessmentTestFixtures.getInsufficientEvidenceSet()
        assertEquals(AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.AUDIO_UNAVAILABLE),
            AssessmentRules.assess(sample.first, sample.second))
    }

    private fun assertThreshold(expected: AssessmentResult, vararg values: Boolean) {
        assertEquals(expected, AssessmentRules.calculateResult(
            SpeakingCriteriaResult(values[0], values[1], values[2], values[3], values[4]), emptyList(), emptyMap()
        ))
    }

    private fun assertProblem(input: AssessmentEvidence, reason: InsufficientEvidenceReason) {
        assertEquals(reason, AssessmentRules.findEvidenceProblem(input))
        assertEquals(AssessmentOutcome.InsufficientEvidence(reason), AssessmentRules.assess(input, evaluation))
    }

    private fun assertInvalid(input: AssessmentEvidence, result: SpeakingEvaluation) {
        assertEquals(InsufficientEvidenceReason.EVALUATION_INVALID, AssessmentRules.findEvaluationProblem(input, result))
        assertEquals(AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.EVALUATION_INVALID), AssessmentRules.assess(input, result))
    }

    private fun assertEvidenceSet(sample: Pair<AssessmentEvidence, SpeakingEvaluation>, expected: AssessmentResult) {
        assertEquals(expected, (AssessmentRules.assess(sample.first, sample.second) as AssessmentOutcome.Evaluated).result)
    }
}
