package com.project.speakmate.data.repository

import com.project.speakmate.data.session.AssessmentGradingManager
import com.project.speakmate.domain.model.AssessmentTask
import com.project.speakmate.domain.model.CallSessionState
import com.project.speakmate.domain.model.AssessmentEvidence
import com.project.speakmate.domain.repository.AssessmentGradingRepository
import javax.inject.Inject
import java.util.UUID

class AssessmentGradingRepositoryImpl @Inject constructor(
    private val manager: AssessmentGradingManager
) : AssessmentGradingRepository {
    override val gradingState = manager.gradingState
    override suspend fun grade(evidence: AssessmentEvidence) = manager.grade(evidence)
    override suspend fun gradeCallResult(
        task: AssessmentTask,
        session: CallSessionState,
        userId: String,
        expiresAtMillis: Long
    ) = grade(
        AssessmentEvidence(
            targetLevel = task.targetLevel,
            communicativeGoal = task.communicativeGoal,
            requiredActions = emptyList(),
            callOutcome = checkNotNull(session.outcome),
            failure = session.failure,
            hasUserSpeech = session.hasUserSpeech,
            transcript = session.transcript.toList(),
            audioDurationMillis = session.assessmentAudioDurationMillis,
            assessmentAudioId = session.assessmentAudioId,
            taskId = task.taskId,
            expiresAtMillis = expiresAtMillis,
            userId = userId,
            operationId = UUID.randomUUID().toString()
        )
    )
    override suspend fun retry() = manager.retry()
    override fun clear() = manager.clear()
}
