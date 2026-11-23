package com.project.speakmate.domain.rules

import com.project.speakmate.data.session.PracticeCompletionTestFixtures
import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.AssessmentOutcome
import com.project.speakmate.domain.model.InsufficientEvidenceReason
import com.project.speakmate.domain.model.CallActivityContext
import com.project.speakmate.domain.model.CallActivityType
import com.project.speakmate.domain.model.SenderType
import com.project.speakmate.domain.model.TranscriptMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeCompletionRulesTest {
    companion object {
        private const val ATTEMPT_ID = "attempt"
    }

    @Test
    fun finalTranscriptFiltersPartialAndBlankTurnsAndKeepsInterruption() {
        val messages = listOf(
            TranscriptMessage("partial", SenderType.USER, "unfinished", 0, false, false),
            TranscriptMessage("blank", SenderType.USER, "   ", 1, true, false),
            TranscriptMessage("ai", SenderType.AI, " Hello ", 2, true, true),
            TranscriptMessage("user", SenderType.USER, "Coffee please", 3, true, false)
        )
        val final = PracticeCompletionRules.getFinalTranscripts(messages)

        assertEquals(listOf("Hello", "Coffee please"), final.map { it.content })
        assertEquals(listOf(0, 1), final.map { it.orderIndex })
        assertTrue(final.first().isInterrupted)
    }

    @Test
    fun completedTrainingKeepsRequiredActionDescriptionsForHistory() {
        val completion = PracticeCompletionRules.createCompletion(
            attemptId = ATTEMPT_ID,
            practice = PracticeCompletionTestFixtures.practice(),
            outcome = PracticeCompletionTestFixtures.evaluated()
        )

        assertEquals(
            PracticeCompletionTestFixtures.training.requiredActions.associate { it.requiredActionId to it.description },
            completion.attempt.requiredActionDescriptions
        )
    }

    @Test
    fun anUnfinishedRequiredActionOverridesTheSpeakingThreshold() {
        val completion = PracticeCompletionRules.createCompletion(
            ATTEMPT_ID,
            PracticeCompletionTestFixtures.practice(),
            PracticeCompletionTestFixtures.evaluated(isActionCompleted = false)
        )

        assertEquals(AssessmentResult.FAIL, completion.attempt.result)
    }

    @Test
    fun anUnknownRequiredActionDoesNotBecomeFail() {
        val evaluated = PracticeCompletionTestFixtures.evaluated().copy(requiredActionResults = emptyMap())
        val completion = PracticeCompletionRules.createCompletion(ATTEMPT_ID, PracticeCompletionTestFixtures.practice(), evaluated)

        assertNull(completion.attempt.result)
        assertNull(completion.attempt.criteriaResult)
        assertTrue(completion.attempt.requiredActionResults.isEmpty())
    }

    @Test
    fun theResultIsCalculatedFromCriteriaRatherThanTheSuppliedLabel() {
        val evaluated = PracticeCompletionTestFixtures.evaluated().copy(result = AssessmentResult.FAIL)
        val completion = PracticeCompletionRules.createCompletion(ATTEMPT_ID, PracticeCompletionTestFixtures.practice(), evaluated)

        assertEquals(AssessmentResult.PASS, completion.attempt.result)
    }

    @Test
    fun datesComeFromTheManagersEpochAnchorAndTerminalInterval() {
        val completion = PracticeCompletionRules.createCompletion(ATTEMPT_ID, PracticeCompletionTestFixtures.practice(), null)

        assertEquals(PracticeCompletionTestFixtures.EPOCH_START_MILLIS, completion.attempt.startedAt.time)
        assertEquals(PracticeCompletionTestFixtures.END_MILLIS - PracticeCompletionTestFixtures.START_MILLIS, completion.attempt.endedAt.time - completion.attempt.startedAt.time)
    }

    @Test
    fun placementAndReassessmentCannotBecomePracticeHistory() {
        val practice = PracticeCompletionTestFixtures.practice()
        listOf(CallActivityType.INITIAL_PLACEMENT, CallActivityType.REASSESSMENT).forEach { activity ->
            assertFalse(PracticeCompletionRules.canComplete(practice.copy(context = CallActivityContext.Assessment(PracticeCompletionTestFixtures.scenario, activity, ""))))
        }
    }

    @Test
    fun onlyTechnicalFailuresCanRetryTheSameEvidence() {
        InsufficientEvidenceReason.entries.forEach { reason ->
            assertEquals(reason == InsufficientEvidenceReason.EVALUATION_FAILED || reason == InsufficientEvidenceReason.EVALUATION_INVALID,
                PracticeCompletionRules.canRetryGrading(reason))
        }
    }

    @Test
    fun evidenceExpiresAtTheOriginalDeadlineAndRejectsClockRollback() {
        val created = PracticeCompletionTestFixtures.EPOCH_START_MILLIS
        assertFalse(PracticeCompletionRules.isEvidenceExpired(created, created + PracticeCompletionRules.AUDIO_RETENTION_MILLIS - 1))
        assertTrue(PracticeCompletionRules.isEvidenceExpired(created, created + PracticeCompletionRules.AUDIO_RETENTION_MILLIS))
        assertTrue(PracticeCompletionRules.isEvidenceExpired(created, created - 1))
        assertTrue(PracticeCompletionRules.isEvidenceExpired(Long.MIN_VALUE, Long.MAX_VALUE))
    }

    @Test
    fun explicitlyUnassessedResultsHaveNullCriteriaAndReasonSpecificFeedback() {
        val failures = listOf(InsufficientEvidenceReason.EVALUATION_FAILED, InsufficientEvidenceReason.EVALUATION_INVALID,
            InsufficientEvidenceReason.AUDIO_UNAVAILABLE, InsufficientEvidenceReason.NOT_ENOUGH_SPEECH)
        val completions = failures.map { reason -> PracticeCompletionRules.createCompletion(ATTEMPT_ID,
            PracticeCompletionTestFixtures.practice(), AssessmentOutcome.InsufficientEvidence(reason)) }
        completions.forEach {
            assertNull(it.attempt.result)
            assertNull(it.attempt.criteriaResult)
            assertTrue(it.attempt.requiredActionResults.isEmpty())
            assertFalse(it.attempt.feedbackSummary.isNullOrBlank())
        }
        assertEquals(failures.size, completions.map { it.attempt.feedbackSummary }.distinct().size)
    }

}
