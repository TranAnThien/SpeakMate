package com.project.speakmate.domain.model

data class PracticeHistoryPage(
    val attempts: List<PracticeAttempt> = emptyList(),
    val nextCursor: PracticeHistoryCursor? = null,
    val hasMore: Boolean = false
)
