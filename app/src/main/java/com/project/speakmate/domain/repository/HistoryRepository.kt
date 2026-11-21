package com.project.speakmate.domain.repository

import com.project.speakmate.domain.model.PracticeAttempt
import com.project.speakmate.domain.model.PracticeCompletion
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    suspend fun savePracticeAttempt(attempt: PracticeAttempt): Result<Unit>
    suspend fun saveCompletedPractice(userId: String, completion: PracticeCompletion): Result<Unit>
    fun observePracticeHistory(): Flow<List<PracticeAttempt>>
    suspend fun getSessionDetail(attemptId: String): Result<PracticeAttempt>
}
