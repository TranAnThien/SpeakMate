package com.project.speakmate.data.session

import com.project.speakmate.di.ApplicationScope
import com.project.speakmate.domain.model.AssessmentEvidence
import com.project.speakmate.domain.model.AssessmentOutcome
import com.project.speakmate.domain.model.CallActivityContext
import com.project.speakmate.domain.model.CallActivityType
import com.project.speakmate.domain.model.CompletedPractice
import com.project.speakmate.domain.model.InsufficientEvidenceReason
import com.project.speakmate.domain.model.PracticeCompletion
import com.project.speakmate.domain.model.PracticeCompletionState
import com.project.speakmate.domain.model.PendingPracticeCompletion
import com.project.speakmate.domain.model.PracticeCompletionStatus
import com.project.speakmate.domain.model.PracticeType
import com.project.speakmate.domain.repository.AuthRepository
import com.project.speakmate.domain.repository.HistoryRepository
import com.project.speakmate.domain.repository.SpeakingAssessmentRepository
import com.project.speakmate.domain.rules.PracticeCompletionRules
import com.project.speakmate.utils.extension.defaultEmpty
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class PracticeCompletionManager @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val authRepository: AuthRepository,
    private val historyRepository: HistoryRepository,
    private val assessmentRepository: SpeakingAssessmentRepository
) {
    private val entries = ConcurrentHashMap<String, CompletionEntry>()
    private val pendingLock = Any()
    private val pendingByUser = MutableStateFlow<Map<String, List<PendingPracticeCompletion>>>(emptyMap())

    init {
        applicationScope.launch {
            authRepository.observeSignedInUserId().distinctUntilChanged().catch { failure ->
                if (failure is CancellationException) throw failure
                clearOtherUsers(null)
            }.collect { clearOtherUsers(it) }
        }
    }

    fun completePractice(practice: CompletedPractice): String? {
        if (!PracticeCompletionRules.canComplete(practice)) return null
        val userId = authRepository.requireCurrentUserId()
        val attemptId = UUID.randomUUID().toString()
        val entryScope = CoroutineScope(applicationScope.coroutineContext + SupervisorJob(applicationScope.coroutineContext[Job]))
        val summary = PendingPracticeCompletion(
            attemptId = attemptId,
            practiceType = when (practice.context.activityType) {
                CallActivityType.FREE_PRACTICE -> PracticeType.FREE_PRACTICE
                CallActivityType.TRAINING_UNIT -> PracticeType.TRAINING_UNIT
                else -> PracticeType.GOAL_ASSESSMENT
            },
            scenarioTitle = practice.context.scenario.name,
            startedAtEpochMillis = requireNotNull(practice.session.startedAtEpochMillis),
            status = PracticeCompletionStatus.GRADING
        )
        val entry = CompletionEntry(userId, entryScope, summary)
        val session = practice.session.copy(transcript = practice.session.transcript.toList())
        val snapshot = practice.copy(session = session)
        entries[attemptId] = entry
        publishPending()
        entry.scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                check(authRepository.requireCurrentUserId() == userId)
                val outcome = evaluate(snapshot)
                currentCoroutineContext().ensureActive()
                val completion = PracticeCompletionRules.createCompletion(attemptId, snapshot, outcome)
                save(attemptId, entry, completion)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                entries.remove(attemptId, entry)
                entry.state.value = PracticeCompletionState.Unavailable
                entry.scope.cancel()
                publishPending()
            } finally {
                session.assessmentAudioId?.let { clearAudio(it) }
            }
        }
        return attemptId
    }

    fun observeCompletion(attemptId: String): Flow<PracticeCompletionState> =
        entries[attemptId]?.state?.asStateFlow() ?: flowOf(PracticeCompletionState.Unavailable)

    fun observePendingCompletions(): Flow<List<PendingPracticeCompletion>> = combine(
        pendingByUser,
        authRepository.observeSignedInUserId().catch { failure ->
            if (failure is CancellationException) throw failure
            emit(null)
        }
    ) { pending, userId -> pending[userId].defaultEmpty() }.distinctUntilChanged()

    fun retrySave(attemptId: String) {
        val entry = entries[attemptId] ?: return
        if (runCatching { authRepository.requireCurrentUserId() }.getOrNull() != entry.userId) return
        val failed = entry.state.value as? PracticeCompletionState.SaveFailed ?: return
        if (failed.isSaving || !entry.state.compareAndSet(failed, failed.copy(isSaving = true))) return
        entry.status = PracticeCompletionStatus.SAVING
        publishPending()
        entry.scope.launch { save(attemptId, entry, failed.completion) }
    }

    fun releaseCompletion(attemptId: String) {
        val entry = entries[attemptId] ?: return
        entry.isRead.set(true)
        removeSaved(attemptId, entry)
    }

    private suspend fun evaluate(practice: CompletedPractice): AssessmentOutcome? {
        val training = (practice.context as? CallActivityContext.Training)?.activityScenario ?: return null
        return try {
            assessmentRepository.evaluate(
                AssessmentEvidence(
                    targetLevel = training.targetLevel,
                    communicativeGoal = training.communicativeGoal,
                    requiredActions = training.requiredActions.toList(),
                    callOutcome = requireNotNull(practice.session.outcome),
                    failure = practice.session.failure,
                    hasUserSpeech = practice.session.hasUserSpeech,
                    transcript = PracticeCompletionRules.getFinalTranscripts(practice.session.transcript),
                    audioDurationMillis = practice.session.assessmentAudioDurationMillis,
                    assessmentAudioId = practice.session.assessmentAudioId
                )
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.EVALUATION_FAILED)
        }
    }

    private suspend fun save(attemptId: String, entry: CompletionEntry, completion: PracticeCompletion) {
        entry.mutex.withLock {
            if (entries[attemptId] !== entry || entry.state.value is PracticeCompletionState.Saved) return
            try {
                check(authRepository.requireCurrentUserId() == entry.userId)
                entry.status = PracticeCompletionStatus.SAVING
                publishPending()
                historyRepository.saveCompletedPractice(entry.userId, completion).getOrThrow()
                currentCoroutineContext().ensureActive()
                if (entries[attemptId] !== entry) return
                entry.state.value = PracticeCompletionState.Saved(completion)
                publishPending()
                removeSaved(attemptId, entry)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (entries[attemptId] === entry) {
                    entry.state.value = PracticeCompletionState.SaveFailed(completion)
                    entry.status = PracticeCompletionStatus.SAVE_FAILED
                    publishPending()
                }
            }
        }
    }

    private fun removeSaved(attemptId: String, entry: CompletionEntry) {
        if (entry.isRead.get() && entry.state.value is PracticeCompletionState.Saved && entries.remove(attemptId, entry)) {
            entry.scope.cancel()
            publishPending()
        }
    }

    private fun clearOtherUsers(userId: String?) {
        entries.forEach { (attemptId, entry) ->
            if (entry.userId != userId && entries.remove(attemptId, entry)) {
                entry.state.value = PracticeCompletionState.Unavailable
                entry.scope.cancel()
            }
        }
        publishPending()
    }

    private fun publishPending() {
        synchronized(pendingLock) {
            pendingByUser.value = entries.values
                .filter { it.state.value != PracticeCompletionState.Unavailable && it.state.value !is PracticeCompletionState.Saved }
                .groupBy { it.userId }
                .mapValues { (_, pending) ->
                    pending.map { it.summary.copy(status = it.status) }
                        .sortedWith(compareByDescending<PendingPracticeCompletion> { it.startedAtEpochMillis }.thenBy { it.attemptId })
                }
        }
    }

    private suspend fun clearAudio(audioId: String) {
        withContext(NonCancellable) {
            try {
                assessmentRepository.clearAudioEvidence(audioId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                Unit
            }
        }
    }

    private class CompletionEntry(val userId: String, val scope: CoroutineScope, val summary: PendingPracticeCompletion) {
        val state = MutableStateFlow<PracticeCompletionState>(PracticeCompletionState.Grading)
        val isRead = AtomicBoolean(false)
        val mutex = Mutex()
        @Volatile
        var status = PracticeCompletionStatus.GRADING
    }
}
