package com.project.speakmate.domain.repository

import com.project.speakmate.domain.model.CompletedPractice
import com.project.speakmate.domain.model.PendingPracticeCompletion
import com.project.speakmate.domain.model.PracticeCompletionState
import kotlinx.coroutines.flow.Flow

interface PracticeCompletionRepository {
    fun completePractice(practice: CompletedPractice): String?
    fun observeCompletion(attemptId: String): Flow<PracticeCompletionState>
    fun observePendingCompletions(): Flow<List<PendingPracticeCompletion>>
    fun retrySave(attemptId: String)
    fun releaseCompletion(attemptId: String)
}
