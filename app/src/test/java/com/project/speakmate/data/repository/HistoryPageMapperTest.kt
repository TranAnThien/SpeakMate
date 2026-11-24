package com.project.speakmate.data.repository

import com.project.speakmate.domain.model.PracticeAttempt
import com.project.speakmate.domain.model.PracticeType
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryPageMapperTest {
    companion object {
        private const val PAGE_SIZE = 20
    }
    private val attempt = PracticeAttempt(attemptId = "attempt", scenarioId = "scenario", practiceType = PracticeType.TRAINING_UNIT,
        startedAt = Date(0), endedAt = Date(0), result = null, criteriaResult = null,
        requiredActionResults = emptyMap(), transcripts = emptyList(), feedbackSummary = null)

    @Test
    fun fullPageDisplaysAllTwentyAndKeepsThePreciseOwnedCursor() {
        val entries = List(20) { index -> HistoryPageEntry("attempt-$index", 1L, 123456789 - index, attempt.copy(attemptId = "attempt-$index"), false) }
        val page = HistoryPageMapper.toPage(entries, "user", PracticeType.TRAINING_UNIT, PAGE_SIZE)
        assertEquals(entries.map { it.attemptId }, page.attempts.map { it.attemptId })
        assertTrue(page.hasMore)
        val cursor = requireNotNull(page.nextCursor)
        assertEquals(1L, cursor.startedAtSeconds)
        assertEquals(123456770, cursor.startedAtNanoseconds)
        assertEquals("attempt-19", cursor.attemptId)
        assertEquals("user", cursor.userId)
        assertEquals(PracticeType.TRAINING_UNIT, cursor.practiceType)
    }

    @Test
    fun invalidAndPendingRowsAdvanceTheRawBoundaryWithoutBecomingSaved() {
        val entries = List(20) { index -> HistoryPageEntry("attempt-$index", 1L, 20 - index, null, index % 2 == 0) }
        val page = HistoryPageMapper.toPage(entries, "user", null, PAGE_SIZE)
        assertTrue(page.attempts.isEmpty())
        assertEquals("attempt-19", page.nextCursor?.attemptId)
        assertTrue(page.hasMore)
    }

    @Test
    fun datasetSizesUseOnlyTwentyDocumentsAndAnEmptyReadForExactMultiples() {
        listOf(0, 1, 19, 20, 21, 40, 41).forEach { count ->
            val entries = List(count) { index -> HistoryPageEntry("attempt-$index", 1L, count - index, attempt.copy(attemptId = "attempt-$index"), false) }
            var offset = 0
            var requests = 0
            val ids = mutableListOf<String>()
            do {
                val page = HistoryPageMapper.toPage(entries.drop(offset).take(20), "user", null, PAGE_SIZE)
                requests++
                ids.addAll(page.attempts.map { it.attemptId })
                offset += 20
            } while (page.hasMore)
            assertEquals(entries.map { it.attemptId }, ids)
            assertEquals(count / 20 + 1, requests)
        }
    }

    @Test
    fun shortAndEmptyPagesHaveNoCursor() {
        val short = HistoryPageMapper.toPage(listOf(HistoryPageEntry("attempt", 1L, 0, attempt, false)), "user", null, PAGE_SIZE)
        assertFalse(short.hasMore)
        assertNull(short.nextCursor)
        val empty = HistoryPageMapper.toPage(emptyList(), "user", null, PAGE_SIZE)
        assertTrue(empty.attempts.isEmpty())
        assertFalse(empty.hasMore)
        assertNull(empty.nextCursor)
    }

    @Test
    fun missingBoundaryTimestampDoesNotDiscardValidRows() {
        val entries = List(PAGE_SIZE) { index ->
            HistoryPageEntry("attempt-$index", if (index == PAGE_SIZE - 1) null else 1L, 0, attempt, false)
        }
        val page = HistoryPageMapper.toPage(entries, "user", null, PAGE_SIZE)
        assertEquals(PAGE_SIZE, page.attempts.size)
        assertEquals("attempt-18", page.nextCursor?.attemptId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun lookaheadDocumentsAreRejected() {
        HistoryPageMapper.toPage(List(21) { HistoryPageEntry("attempt-$it", 1L, 0, null, false) }, "user", null, PAGE_SIZE)
    }
}
