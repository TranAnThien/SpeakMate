package com.project.speakmate.data.repository

import com.project.speakmate.data.session.PracticeCompletionManager
import com.project.speakmate.domain.model.CompletedPractice
import com.project.speakmate.domain.repository.PracticeCompletionRepository
import javax.inject.Inject

class PracticeCompletionRepositoryImpl @Inject constructor(
    private val manager: PracticeCompletionManager
) : PracticeCompletionRepository {
    override fun setNetworkAvailable(isAvailable: Boolean) = manager.setNetworkAvailable(isAvailable)
    override fun resumePendingSaves() = manager.resumePendingSaves()
    override fun completePractice(practice: CompletedPractice) = manager.completePractice(practice)
    override fun observeCompletion(attemptId: String) = manager.observeCompletion(attemptId)
    override fun observePendingCompletions() = manager.observePendingCompletions()
    override fun retryGrading(attemptId: String) = manager.retryGrading(attemptId)
    override fun saveUnassessed(attemptId: String) = manager.saveUnassessed(attemptId)
    override fun discardCompletion(attemptId: String) = manager.discardCompletion(attemptId)
    override fun retrySave(attemptId: String) = manager.retrySave(attemptId)
    override fun releaseCompletion(attemptId: String) = manager.releaseCompletion(attemptId)
}
