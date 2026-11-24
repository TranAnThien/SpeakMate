package com.project.speakmate.data.repository

import com.project.speakmate.domain.model.SenderType
import com.project.speakmate.domain.model.TranscriptMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryTranscriptMapperTest {
    @Test
    fun finalMessagesKeepStableOrderAndInterruptedEvidence() {
        val later = TranscriptMessage("user", SenderType.USER, "Answer", 2, true, false)
        val earlier = TranscriptMessage("ai", SenderType.AI, "Question", 1, true, true)
        assertEquals(listOf(earlier, later), HistoryTranscriptMapper.toMessages(listOf(later, earlier)))
    }

    @Test
    fun partialAndBlankMessagesYieldAValidEmptyTranscript() {
        val partial = TranscriptMessage("partial", SenderType.USER, "Not final", 0, false, false)
        val blank = TranscriptMessage("blank", SenderType.AI, " ", 1, true, false)
        assertTrue(HistoryTranscriptMapper.toMessages(listOf(partial, blank)).isEmpty())
        assertTrue(HistoryTranscriptMapper.toMessages(emptyList()).isEmpty())
    }

    @Test
    fun equalOrderUsesIdentityForDeterministicDisplay() {
        val first = TranscriptMessage("a", SenderType.AI, "First", 0, true, false)
        val second = TranscriptMessage("b", SenderType.USER, "Second", 0, true, false)
        assertEquals(listOf(first, second), HistoryTranscriptMapper.toMessages(listOf(second, first)))
    }

    @Test
    fun malformedOrDuplicateFinalIdentityCannotBecomeSuccessfulPartialHistory() {
        val message = TranscriptMessage("same", SenderType.AI, "Content", 0, true, false)
        assertThrows(IllegalArgumentException::class.java) { HistoryTranscriptMapper.toMessages(listOf(message, message)) }
        assertThrows(IllegalArgumentException::class.java) { HistoryTranscriptMapper.toMessages(listOf(message.copy(messageId = ""))) }
        assertThrows(IllegalArgumentException::class.java) { HistoryTranscriptMapper.toMessages(listOf(message.copy(orderIndex = -1))) }
    }
}
