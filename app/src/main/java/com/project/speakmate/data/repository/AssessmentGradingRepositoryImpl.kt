package com.project.speakmate.data.repository

import com.project.speakmate.data.session.AssessmentGradingManager
import com.project.speakmate.domain.model.AssessmentEvidence
import com.project.speakmate.domain.repository.AssessmentGradingRepository
import javax.inject.Inject

class AssessmentGradingRepositoryImpl @Inject constructor(
    private val manager: AssessmentGradingManager
) : AssessmentGradingRepository {
    override val gradingState = manager.gradingState
    override suspend fun grade(evidence: AssessmentEvidence) = manager.grade(evidence)
    override suspend fun retry() = manager.retry()
    override fun clear() = manager.clear()
}
