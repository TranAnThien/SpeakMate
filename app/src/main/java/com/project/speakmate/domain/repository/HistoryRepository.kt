package com.project.speakmate.domain.repository

import com.project.speakmate.domain.model.PracticeAttempt
import com.project.speakmate.domain.model.PracticeCompletion
import com.project.speakmate.domain.model.PracticeHistoryCursor
import com.project.speakmate.domain.model.PracticeHistoryPage
import com.project.speakmate.domain.model.PracticeType
import com.project.speakmate.domain.model.TranscriptMessage

interface HistoryRepository {
    suspend fun saveCompletedPractice(userId: String, completion: PracticeCompletion): Result<Unit>

    suspend fun getPracticeHistoryPage(
        practiceType: PracticeType?,
        cursor: PracticeHistoryCursor? = null
    ): Result<PracticeHistoryPage>

    suspend fun getPracticeAttempt(attemptId: String, requireServer: Boolean = false): Result<PracticeAttempt>

    suspend fun getSessionMessages(attemptId: String, requireServer: Boolean = false): Result<List<TranscriptMessage>>

    suspend fun getSessionDetail(attemptId: String, requireServer: Boolean = false): Result<PracticeAttempt>
}
