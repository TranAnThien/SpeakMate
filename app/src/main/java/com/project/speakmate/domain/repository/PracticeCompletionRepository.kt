package com.project.speakmate.domain.repository

import com.project.speakmate.domain.model.CompletedPractice
import com.project.speakmate.domain.model.PendingPracticeCompletion
import com.project.speakmate.domain.model.PracticeCompletionState
import kotlinx.coroutines.flow.Flow

interface PracticeCompletionRepository {
    fun setNetworkAvailable(isAvailable: Boolean)
    fun resumePendingSaves()
    fun completePractice(practice: CompletedPractice): String?
    fun observeCompletion(attemptId: String): Flow<PracticeCompletionState>
    fun observePendingCompletions(): Flow<List<PendingPracticeCompletion>>
    fun retryGrading(attemptId: String)
    fun saveUnassessed(attemptId: String)
    fun discardCompletion(attemptId: String)
    fun retrySave(attemptId: String)
    fun releaseCompletion(attemptId: String)
}
