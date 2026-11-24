package com.project.speakmate.data.session

import com.project.speakmate.domain.model.PracticeAttempt
import com.project.speakmate.domain.model.PracticeCompletion
import com.project.speakmate.domain.model.PracticeHistoryCursor
import com.project.speakmate.domain.model.PracticeHistoryPage
import com.project.speakmate.domain.model.PracticeType
import com.project.speakmate.domain.model.TrainingPath
import com.project.speakmate.domain.model.TrainingProgress
import com.project.speakmate.domain.model.TrainingProgressDecision
import com.project.speakmate.domain.model.TranscriptMessage
import com.project.speakmate.domain.repository.HistoryRepository
import com.project.speakmate.domain.rules.TrainingProgressRules
import kotlinx.coroutines.CompletableDeferred

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
                when (val decision =
                    TrainingProgressRules.applyOutcome(
                        path = currentPath,
                        progress = currentProgress,
                        activity = training.activity,
                        outcome = checkNotNull(completion.outcome)
                    )) {
                    is TrainingProgressDecision.Advanced -> progress = decision.progress
                    is TrainingProgressDecision.PathCompleted -> progress = decision.progress
                    else -> Unit
                }
            }
            attempts[completion.attempt.attemptId] = completion.attempt
        }
        return if (shouldFailAfterCommit) Result.failure(IllegalStateException()) else Result.success(Unit)
    }

    override suspend fun getPracticeHistoryPage(
        practiceType: PracticeType?,
        cursor: PracticeHistoryCursor?
    ): Result<PracticeHistoryPage> =
        Result.success(getHistoryPage(pageSize = 20, practiceType = practiceType, cursor = cursor))

    private fun getHistoryPage(
        pageSize: Int,
        practiceType: PracticeType?,
        cursor: PracticeHistoryCursor?
    ): PracticeHistoryPage {
        val sorted =
            attempts.values
                .filter { practiceType == null || it.practiceType == practiceType }
                .sortedWith(compareByDescending<PracticeAttempt> { it.startedAt }.thenByDescending { it.attemptId })
        val start = if (cursor == null) 0 else sorted.indexOfFirst { it.attemptId == cursor.attemptId } + 1
        val page = sorted.drop(start).take(pageSize)
        val hasMore = page.size == pageSize
        val next =
            page
                .lastOrNull()
                ?.takeIf { hasMore }
                ?.let {
                    val instant = it.startedAt.toInstant()
                    PracticeHistoryCursor(
                        userId = PracticeCompletionTestFixtures.USER_ID,
                        practiceType = practiceType,
                        startedAtSeconds = instant.epochSecond,
                        startedAtNanoseconds = instant.nano,
                        attemptId = it.attemptId
                    )
                }
        return PracticeHistoryPage(attempts = page, nextCursor = next, hasMore = hasMore)
    }

    override suspend fun getPracticeAttempt(attemptId: String, requireServer: Boolean): Result<PracticeAttempt> =
        getSessionDetail(attemptId, requireServer).map { it.copy(transcripts = emptyList()) }

    override suspend fun getSessionMessages(
        attemptId: String,
        requireServer: Boolean
    ): Result<List<TranscriptMessage>> = getSessionDetail(attemptId, requireServer).map { it.transcripts }

    override suspend fun getSessionDetail(attemptId: String, requireServer: Boolean): Result<PracticeAttempt> {
        val attempt = attempts[attemptId] ?: return Result.failure(NoSuchElementException())
        return Result.success(attempt)
    }
}
