package com.project.speakmate.data.repository

import com.project.speakmate.domain.model.PracticeHistoryCursor
import com.project.speakmate.domain.model.PracticeHistoryPage
import com.project.speakmate.domain.model.PracticeType

object HistoryPageMapper {
    fun toPage(entries: List<HistoryPageEntry>, userId: String, practiceType: PracticeType?, pageSize: Int): PracticeHistoryPage {
        require(entries.size <= pageSize)
        val hasMore = entries.size == pageSize
        val cursor = if (hasMore) {
            val boundary = entries.lastOrNull { it.startedAtSeconds != null && it.startedAtNanoseconds != null && it.attemptId.isNotBlank() }
            boundary?.let { PracticeHistoryCursor(userId, practiceType, checkNotNull(it.startedAtSeconds),
                checkNotNull(it.startedAtNanoseconds), it.attemptId) }
        } else null
        val attempts = entries.filterNot { it.hasPendingWrites }.mapNotNull { it.attempt }
        return PracticeHistoryPage(attempts, cursor, hasMore && cursor != null)
    }
}
