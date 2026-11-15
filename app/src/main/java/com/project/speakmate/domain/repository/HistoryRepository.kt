package com.project.speakmate.domain.repository

import com.project.speakmate.domain.model.PracticeAttempt
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    suspend fun savePracticeAttempt(attempt: PracticeAttempt): Result<Unit>
    fun getPracticeHistoryList(): Flow<List<PracticeAttempt>>
    suspend fun getSessionDetail(attemptId: String): Result<PracticeAttempt>
}
