package com.project.speakmate.data.session

import com.project.speakmate.di.ApplicationScope
import com.project.speakmate.domain.model.AssessmentEvidence
import com.project.speakmate.domain.model.AssessmentOutcome
import com.project.speakmate.domain.model.CallActivityContext
import com.project.speakmate.domain.model.CallActivityType
import com.project.speakmate.domain.model.CompletedPractice
import com.project.speakmate.domain.model.InsufficientEvidenceReason
import com.project.speakmate.domain.model.PendingPracticeCompletion
import com.project.speakmate.domain.model.PracticeCompletion
import com.project.speakmate.domain.model.PracticeCompletionState
import com.project.speakmate.domain.model.PracticeCompletionStatus
import com.project.speakmate.domain.model.PracticeSaveException
import com.project.speakmate.domain.model.PracticeSaveFailure
import com.project.speakmate.domain.model.PracticeType
import com.project.speakmate.domain.repository.AuthRepository
import com.project.speakmate.domain.repository.HistoryRepository
import com.project.speakmate.domain.repository.SpeakingAssessmentRepository
import com.project.speakmate.domain.rules.PracticeCompletionRules
import com.project.speakmate.domain.rules.PracticeSaveRetryRules
import com.project.speakmate.utils.extension.default
import com.project.speakmate.utils.extension.defaultEmpty
import java.time.Clock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

@Singleton
class PracticeCompletionManager
@Inject
constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val authRepository: AuthRepository,
    private val historyRepository: HistoryRepository,
    private val assessmentRepository: SpeakingAssessmentRepository,
    private val clock: Clock
) {
    companion object {
        private const val SAVE_TIMEOUT_MILLIS = 30_000L
    }

    private val entries = ConcurrentHashMap<String, CompletionEntry>()
    private val pendingLock = Any()
    private val pendingByUser = MutableStateFlow<Map<String, List<PendingPracticeCompletion>>>(emptyMap())
    @Volatile private var isNetworkAvailable = false

    init {
        applicationScope.launch {
            authRepository
                .observeSignedInUserId()
                .distinctUntilChanged()
                .catch { failure ->
                    if (failure is CancellationException) throw failure
                    clearOtherUsers(userId = null)
                }
                .collect { uid -> clearOtherUsers(uid) }
        }
    }

    fun setNetworkAvailable(isAvailable: Boolean) {
        if (isNetworkAvailable == isAvailable) return
        isNetworkAvailable = isAvailable
        if (isAvailable) {
            entries.values.forEach { entry ->
                synchronized(entry) {
                    val failed = entry.state.value as? PracticeCompletionState.SaveFailed
                    if (failed != null && !failed.isSaving && entry.automaticRetryJob?.isActive != true)
                        entry.automaticAttempts = 0
                }
            }
            resumePendingSaves()
        } else
            entries.values.forEach {
                it.automaticRetryJob?.cancel()
                it.automaticRetryJob = null
            }
    }

    fun resumePendingSaves() {
        if (!isNetworkAvailable) return
        entries.forEach { (id, entry) ->
            val state = entry.state.value as? PracticeCompletionState.SaveFailed ?: return@forEach
            if (!state.isSaving &&
                entry.automaticRetryJob?.isActive != true &&
                PracticeSaveRetryRules.canRetry(state.reason)) {
                scheduleRetry(id, entry)
            }
        }
    }

    private fun scheduleRetry(attemptId: String, entry: CompletionEntry) {
        val failed = entry.state.value as? PracticeCompletionState.SaveFailed ?: return
        if (failed.isSaving ||
            !isNetworkAvailable ||
            !PracticeSaveRetryRules.canRetry(failed.reason) ||
            ownedEntry(attemptId) !== entry)
            return
        synchronized(entry) {
            if (entry.automaticRetryJob?.isActive == true) return
            val delayMillis = PracticeSaveRetryRules.getDelayMillis(entry.automaticAttempts) ?: return
            entry.automaticRetryJob =
                entry.scope.launch {
                    delay(delayMillis)
                    entry.automaticRetryJob = null
                    if (!isNetworkAvailable || ownedEntry(attemptId) !== entry) return@launch
                    val pending = entry.state.value as? PracticeCompletionState.SaveFailed ?: return@launch
                    if (pending.isSaving ||
                        !PracticeSaveRetryRules.canRetry(pending.reason) ||
                        !entry.state.compareAndSet(pending, pending.copy(isSaving = true)))
                        return@launch
                    entry.automaticAttempts++
                    entry.status = PracticeCompletionStatus.SAVING
                    publishPending()
                    entry.scope.launch { save(attemptId = attemptId, entry = entry, completion = pending.completion) }
                }
        }
    }

    fun discardCompletion(attemptId: String) {
        val entry = ownedEntry(attemptId) ?: return
        val state = entry.state.value
        if (state !is PracticeCompletionState.AwaitingGrading &&
            (state !is PracticeCompletionState.SaveFailed || state.isSaving))
            return
        if (!entry.state.compareAndSet(state, PracticeCompletionState.Unavailable)) return
        entries.remove(attemptId, entry)
        entry.scope.cancel()
        publishPending()
        applicationScope.launch { clearEntryAudio(entry) }
    }

    fun completePractice(practice: CompletedPractice): String? =
        synchronized(pendingLock) {
            if (!PracticeCompletionRules.canComplete(practice)) return null
            val userId =
                try {
                    authRepository.requireCurrentUserId()
                } catch (_: IllegalStateException) {
                    return null
                }
            entries.entries
                .firstOrNull { (_, entry) ->
                    entry.userId == userId &&
                        entry.practice.session.startedAtEpochMillis == practice.session.startedAtEpochMillis &&
                        entry.practice.session.startedAtMillis == practice.session.startedAtMillis &&
                        entry.practice.context == practice.context
                }
                ?.let {
                    return it.key
                }
            val attemptId = UUID.randomUUID().toString()
            val entryScope =
                CoroutineScope(
                    applicationScope.coroutineContext + SupervisorJob(applicationScope.coroutineContext[Job])
                )
            val summary =
                PendingPracticeCompletion(
                    attemptId = attemptId,
                    practiceType =
                        when (practice.context.activityType) {
                            CallActivityType.FREE_PRACTICE -> PracticeType.FREE_PRACTICE
                            CallActivityType.TRAINING_UNIT -> PracticeType.TRAINING_UNIT
                            else -> PracticeType.GOAL_ASSESSMENT
                        },
                    scenarioTitle = practice.context.scenario.name,
                    startedAtEpochMillis = requireNotNull(practice.session.startedAtEpochMillis),
                    status = PracticeCompletionStatus.GRADING
                )
            val session = practice.session.copy(transcript = practice.session.transcript.toList())
            val context =
                when (val current = practice.context) {
                    is CallActivityContext.Training ->
                        current.copy(
                            activityScenario =
                                current.activityScenario.copy(
                                    requiredActions = current.activityScenario.requiredActions.toList()
                                )
                        )
                    else -> current
                }
            val snapshot = practice.copy(session = session, context = context)
            val entry =
                CompletionEntry(
                    userId = userId,
                    scope = entryScope,
                    summary = summary,
                    practice = snapshot,
                    createdAtMillis = clock.millis()
                )
            entries[attemptId] = entry
            publishPending()
            entry.expiryJob =
                entry.scope.launch {
                    delay(PracticeCompletionRules.AUDIO_RETENTION_MILLIS)
                    clearEntryAudio(entry)
                }
            entry.scope.launch { grade(attemptId, entry) }
            return attemptId
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeCompletion(attemptId: String): Flow<PracticeCompletionState> {
        val entry = ownedEntry(attemptId)
        return authRepository
            .observeSignedInUserId()
            .distinctUntilChanged()
            .flatMapLatest { uid ->
                if (entry == null || entry.userId != uid) flowOf(PracticeCompletionState.Unavailable)
                else
                    entry.state.map { state ->
                        val current =
                            try {
                                authRepository.requireCurrentUserId()
                            } catch (_: IllegalStateException) {
                                null
                            }
                        if (current == entry.userId) state else PracticeCompletionState.Unavailable
                    }
            }
            .distinctUntilChanged()
    }

    fun observePendingCompletions(): Flow<List<PendingPracticeCompletion>> =
        combine(pendingByUser, authRepository.observeSignedInUserId()) { pending, uid ->
            val current =
                try {
                    authRepository.requireCurrentUserId()
                } catch (_: IllegalStateException) {
                    null
                }
            if (uid != null && uid == current) pending[uid].defaultEmpty() else emptyList()
        }
            .distinctUntilChanged()

    fun retryGrading(attemptId: String) {
        val entry = ownedEntry(attemptId) ?: return
        val pending = entry.state.value as? PracticeCompletionState.AwaitingGrading ?: return
        if (!entry.state.compareAndSet(pending, PracticeCompletionState.Grading)) return
        entry.status = PracticeCompletionStatus.GRADING
        publishPending()
        entry.scope.launch { grade(attemptId, entry) }
    }

    fun saveUnassessed(attemptId: String) {
        val entry = ownedEntry(attemptId) ?: return
        val pending = entry.state.value as? PracticeCompletionState.AwaitingGrading ?: return
        val reason =
            if (PracticeCompletionRules.isEvidenceExpired(entry.createdAtMillis, clock.millis()))
                InsufficientEvidenceReason.AUDIO_UNAVAILABLE
            else pending.reason
        val completion =
            PracticeCompletionRules.createCompletion(
                attemptId = attemptId,
                practice = entry.practice,
                outcome = AssessmentOutcome.InsufficientEvidence(reason)
            )
        if (!entry.state.compareAndSet(pending, PracticeCompletionState.Saving(completion))) return
        entry.expiryJob?.cancel()
        entry.scope.launch {
            clearEntryAudio(entry)
            save(attemptId = attemptId, entry = entry, completion = completion)
        }
    }

    private fun ownedEntry(attemptId: String): CompletionEntry? {
        val entry = entries[attemptId] ?: return null
        return try {
            entry.takeIf { authRepository.requireCurrentUserId() == it.userId }
        } catch (_: IllegalStateException) {
            null
        }
    }

    private suspend fun grade(attemptId: String, entry: CompletionEntry) {
        var retainAudio = false
        try {
            check(ownedEntry(attemptId) === entry)
            val createdAt = entry.createdAtMillis
            val expired = PracticeCompletionRules.isEvidenceExpired(createdAt, clock.millis())
            val outcome =
                if (expired) AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.AUDIO_UNAVAILABLE)
                else evaluate(entry, attemptId)
            currentCoroutineContext().ensureActive()
            if (ownedEntry(attemptId) !== entry) return
            val completedOutcome =
                if (entry.practice.context is CallActivityContext.Training &&
                    PracticeCompletionRules.isEvidenceExpired(createdAt, clock.millis()))
                    AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.AUDIO_UNAVAILABLE)
                else outcome
            val completion =
                PracticeCompletionRules.createCompletion(
                    attemptId = attemptId,
                    practice = entry.practice,
                    outcome = completedOutcome
                )
            val normalized = completion.outcome
            if (normalized is AssessmentOutcome.InsufficientEvidence &&
                PracticeCompletionRules.canRetryGrading(normalized.reason) &&
                !expired) {
                retainAudio = true
                awaitGrading(attemptId = attemptId, entry = entry, reason = normalized.reason)
            } else {
                save(attemptId = attemptId, entry = entry, completion = completion)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            if (ownedEntry(attemptId) === entry) {
                retainAudio = true
                awaitGrading(
                    attemptId = attemptId,
                    entry = entry,
                    reason = InsufficientEvidenceReason.EVALUATION_FAILED
                )
            }
        } finally {
            if (!retainAudio) clearEntryAudio(entry)
        }
    }

    private fun awaitGrading(attemptId: String, entry: CompletionEntry, reason: InsufficientEvidenceReason) {
        entry.status = PracticeCompletionStatus.AWAITING_GRADING
        entry.state.value =
            PracticeCompletionState.AwaitingGrading(
                reason,
                entry.createdAtMillis + PracticeCompletionRules.AUDIO_RETENTION_MILLIS
            )
        publishPending()
        entry.expiryJob?.cancel()
        entry.expiryJob =
            entry.scope.launch {
                delay(
                    (entry.createdAtMillis + PracticeCompletionRules.AUDIO_RETENTION_MILLIS - clock.millis()).coerceIn(
                        0L,
                        PracticeCompletionRules.AUDIO_RETENTION_MILLIS
                    )
                )
                if (ownedEntry(attemptId) !== entry) return@launch
                clearEntryAudio(entry)
                val pending = entry.state.value as? PracticeCompletionState.AwaitingGrading ?: return@launch
                val completion =
                    PracticeCompletionRules.createCompletion(
                        attemptId = attemptId,
                        practice = entry.practice,
                        outcome = AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.AUDIO_UNAVAILABLE)
                    )
                if (entry.state.compareAndSet(pending, PracticeCompletionState.Saving(completion))) {
                    save(attemptId = attemptId, entry = entry, completion = completion)
                }
            }
    }

    fun retrySave(attemptId: String) {
        val entry = ownedEntry(attemptId) ?: return
        val failed = entry.state.value as? PracticeCompletionState.SaveFailed ?: return
        if (failed.isSaving ||
            !PracticeSaveRetryRules.canRetry(failed.reason) ||
            !entry.state.compareAndSet(failed, failed.copy(isSaving = true)))
            return
        entry.automaticRetryJob?.cancel()
        entry.automaticRetryJob = null
        entry.status = PracticeCompletionStatus.SAVING
        publishPending()
        entry.scope.launch { save(attemptId = attemptId, entry = entry, completion = failed.completion) }
    }

    fun releaseCompletion(attemptId: String) {
        val entry = ownedEntry(attemptId) ?: return
        entry.isRead.set(true)
        removeSaved(attemptId, entry)
    }

    private suspend fun evaluate(entry: CompletionEntry, attemptId: String): AssessmentOutcome? {
        val practice = entry.practice
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
                    assessmentAudioId = practice.session.assessmentAudioId,
                    operationId = attemptId,
                    userId = entry.userId,
                    expiresAtMillis = entry.createdAtMillis + PracticeCompletionRules.AUDIO_RETENTION_MILLIS
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
                if (entry.state.value !is PracticeCompletionState.SaveFailed)
                    entry.state.value = PracticeCompletionState.Saving(completion)
                entry.status = PracticeCompletionStatus.SAVING
                publishPending()
                currentCoroutineContext().ensureActive()
                if (entries[attemptId] !== entry) return
                try {
                    withTimeout(SAVE_TIMEOUT_MILLIS) {
                        historyRepository.saveCompletedPractice(entry.userId, completion).getOrThrow()
                    }
                } catch (timeout: TimeoutCancellationException) {
                    throw PracticeSaveException(PracticeSaveFailure.NETWORK, timeout)
                }
                currentCoroutineContext().ensureActive()
                if (entries[attemptId] !== entry) return
                check(authRepository.requireCurrentUserId() == entry.userId)
                val saving = entry.state.value
                if (saving !is PracticeCompletionState.Saving &&
                    (saving !is PracticeCompletionState.SaveFailed || !saving.isSaving))
                    return
                if (!entry.state.compareAndSet(saving, PracticeCompletionState.Saved(completion))) return
                entry.automaticRetryJob?.cancel()
                publishPending()
                removeSaved(attemptId, entry)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (entries[attemptId] === entry) {
                    val reason = (failure as? PracticeSaveException)?.reason.default(PracticeSaveFailure.UNKNOWN)
                    val saving = entry.state.value
                    if (saving is PracticeCompletionState.Saved || saving == PracticeCompletionState.Unavailable) return
                    if (!entry.state.compareAndSet(
                        saving,
                        PracticeCompletionState.SaveFailed(completion, reason = reason)
                    ))
                        return
                    entry.status = PracticeCompletionStatus.SAVE_FAILED
                    publishPending()
                    scheduleRetry(attemptId, entry)
                }
            }
        }
    }

    private fun removeSaved(attemptId: String, entry: CompletionEntry) {
        if (entry.isRead.get() &&
            entry.state.value is PracticeCompletionState.Saved &&
            entries.remove(attemptId, entry)) {
            entry.scope.cancel()
            publishPending()
        }
    }

    private fun clearOtherUsers(userId: String?) =
        synchronized(pendingLock) {
            entries.forEach { (attemptId, entry) ->
                if (entry.userId != userId && entries.remove(attemptId, entry)) {
                    entry.state.value = PracticeCompletionState.Unavailable
                    entry.scope.cancel()
                    applicationScope.launch { clearEntryAudio(entry) }
                }
            }
            publishPending()
        }

    private fun publishPending() {
        synchronized(pendingLock) {
            val pending =
                entries.values
                    .filter {
                        it.state.value != PracticeCompletionState.Unavailable &&
                            it.state.value !is PracticeCompletionState.Saved
                    }
                    .map {
                        it.userId to
                            it.summary.copy(
                                status = it.status,
                                saveFailure = (it.state.value as? PracticeCompletionState.SaveFailed)?.reason
                            )
                    }
            pendingByUser.value =
                pending
                    .groupBy { it.first }
                    .mapValues { (_, pending) ->
                        pending
                            .map { it.second }
                            .sortedWith(
                                compareByDescending<PendingPracticeCompletion> { it.startedAtEpochMillis }
                                    .thenBy { it.attemptId }
                            )
                    }
        }
    }

    private suspend fun clearEntryAudio(entry: CompletionEntry) {
        val audioId = entry.practice.session.assessmentAudioId ?: return
        if (!entry.isAudioCleared.compareAndSet(false, true)) return
        val cleared =
            withContext(NonCancellable) {
                try {
                    assessmentRepository.clearAudioEvidence(audioId)
                    true
                } catch (cancelled: CancellationException) {
                    entry.isAudioCleared.set(false)
                    throw cancelled
                } catch (_: Exception) {
                    false
                }
            }
        if (!cleared) entry.isAudioCleared.set(false)
    }

    private class CompletionEntry(
        val userId: String,
        val scope: CoroutineScope,
        val summary: PendingPracticeCompletion,
        val practice: CompletedPractice,
        val createdAtMillis: Long
    ) {
        val state = MutableStateFlow<PracticeCompletionState>(PracticeCompletionState.Grading)
        var expiryJob: Job? = null
        @Volatile var automaticRetryJob: Job? = null
        @Volatile var automaticAttempts = 0
        val isRead = AtomicBoolean(false)
        val isAudioCleared = AtomicBoolean(false)
        val mutex = Mutex()
        @Volatile var status = PracticeCompletionStatus.GRADING
    }
}
