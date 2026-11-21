package com.project.speakmate.data.session

import com.project.speakmate.domain.model.PracticeAttempt
import com.project.speakmate.domain.model.PracticeCompletion
import com.project.speakmate.domain.model.TrainingPath
import com.project.speakmate.domain.model.TrainingProgress
import com.project.speakmate.domain.model.TrainingProgressDecision
import com.project.speakmate.domain.repository.HistoryRepository
import com.project.speakmate.domain.rules.TrainingProgressRules
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flowOf

class FakeHistoryRepository : HistoryRepository {
    val attempts = mutableMapOf<String, PracticeAttempt>()
    val savedIds = mutableListOf<String>()
    var failure: Exception? = null
    var gate: CompletableDeferred<Unit>? = null
    var shouldFailAfterCommit = false
    var path: TrainingPath? = null
    var progress: TrainingProgress? = null

    override suspend fun saveCompletedPractice(userId: String, completion: PracticeCompletion): Result<Unit> {
        savedIds += completion.attempt.attemptId
        gate?.await()
        val exception = failure
        if (exception != null) return Result.failure(exception)
        if (completion.attempt.attemptId !in attempts) {
            val currentPath = path
            val currentProgress = progress
            val training = completion.training
            if (currentPath != null && currentProgress != null && training != null) {
                when (val decision = TrainingProgressRules.applyOutcome(currentPath, currentProgress, training.activity, checkNotNull(completion.outcome))) {
                    is TrainingProgressDecision.Advanced -> progress = decision.progress
                    is TrainingProgressDecision.PathCompleted -> progress = decision.progress
                    else -> Unit
                }
            }
            attempts[completion.attempt.attemptId] = completion.attempt
        }
        return if (shouldFailAfterCommit) Result.failure(IllegalStateException()) else Result.success(Unit)
    }

    override suspend fun savePracticeAttempt(attempt: PracticeAttempt): Result<Unit> {
        attempts[attempt.attemptId] = attempt
        return Result.success(Unit)
    }

    override fun observePracticeHistory() = flowOf(attempts.values.toList())

    override suspend fun getSessionDetail(attemptId: String): Result<PracticeAttempt> {
        val attempt = attempts[attemptId] ?: return Result.failure(NoSuchElementException())
        return Result.success(attempt)
    }
}
