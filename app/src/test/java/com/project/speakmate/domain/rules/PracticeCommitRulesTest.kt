package com.project.speakmate.domain.rules

import com.project.speakmate.data.session.PracticeCompletionTestFixtures
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeCommitRulesTest {
    private val attempt = PracticeCompletionRules.createCompletion(PracticeCompletionTestFixtures.AUDIO_ID,
        PracticeCompletionTestFixtures.practice(), PracticeCompletionTestFixtures.evaluated()).attempt

    @Test
    fun reorderedMessagesMatchTheSamePersistedCompletion() {
        val first = attempt.transcripts.single()
        val expected = attempt.copy(transcripts = listOf(first, first.copy(messageId = first.messageId + first.messageId, orderIndex = 1)))
        assertTrue(PracticeCommitRules.matches(expected, expected.copy(transcripts = expected.transcripts.reversed())))
    }

    @Test
    fun changedResultFeedbackOrTranscriptCannotAcknowledgeTheExpectedCompletion() {
        assertFalse(PracticeCommitRules.matches(attempt, attempt.copy(result = null)))
        assertFalse(PracticeCommitRules.matches(attempt, attempt.copy(feedbackSummary = null)))
        assertFalse(PracticeCommitRules.matches(attempt, attempt.copy(transcripts = emptyList())))
        assertFalse(PracticeCommitRules.matches(attempt, attempt.copy(transcripts = attempt.transcripts.map { it.copy(content = "") })))
    }

    @Test
    fun duplicateMessageIdsOrOrderIndexesCannotBeCommitted() {
        val first = attempt.transcripts.single()
        assertFalse(PracticeCommitRules.hasValidTranscripts(attempt.copy(transcripts = listOf(first, first))))
        assertFalse(PracticeCommitRules.hasValidTranscripts(attempt.copy(transcripts = listOf(first, first.copy(messageId = first.messageId + first.messageId)))))
    }

    @Test
    fun partialMessagesAndInvalidDocumentIdsCannotBeCommitted() {
        listOf(firstMessage().copy(isFinal = false), firstMessage().copy(messageId = ""),
            firstMessage().copy(messageId = "/"), firstMessage().copy(orderIndex = -1)).forEach {
            assertFalse(PracticeCommitRules.hasValidTranscripts(attempt.copy(transcripts = listOf(it))))
        }
    }

    private fun firstMessage() = attempt.transcripts.single()
}
