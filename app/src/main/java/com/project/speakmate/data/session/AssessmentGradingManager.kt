package com.project.speakmate.data.session

import com.project.speakmate.di.ApplicationScope
import com.project.speakmate.domain.model.AssessmentEvidence
import com.project.speakmate.domain.model.AssessmentGradingState
import com.project.speakmate.domain.model.AssessmentOutcome
import com.project.speakmate.domain.model.AssessmentOwnershipException
import com.project.speakmate.domain.model.CallOutcome
import com.project.speakmate.domain.model.InsufficientEvidenceReason
import com.project.speakmate.domain.repository.AuthRepository
import com.project.speakmate.domain.repository.SpeakingAssessmentRepository
import com.project.speakmate.domain.rules.PracticeCompletionRules
import dagger.hilt.android.scopes.ViewModelScoped
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@ViewModelScoped
class AssessmentGradingManager @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val assessmentRepository: SpeakingAssessmentRepository,
    private val authRepository: AuthRepository,
    private val clock: Clock
) {
    private val lock = Any()
    private val _gradingState = MutableStateFlow(AssessmentGradingState())
    val gradingState = _gradingState.asStateFlow()
    private var entry: GradingEntry? = null

    suspend fun grade(evidence: AssessmentEvidence): AssessmentOutcome {
        val current = synchronized(lock) {
            clearEntry()
            val created = GradingEntry(evidence, clock.millis())
            entry = created
            try { ensureOwner(evidence) }
            catch (stale: AssessmentOwnershipException) {
                clearEntry()
                throw stale
            }
            created.expiryJob = applicationScope.launch(start = CoroutineStart.LAZY) {
                val deadline = evidence.expiresAtMillis
                val remaining = if (deadline == null) PracticeCompletionRules.AUDIO_RETENTION_MILLIS
                    else (deadline - created.createdAtMillis).coerceIn(0L, PracticeCompletionRules.AUDIO_RETENTION_MILLIS)
                delay(remaining)
                synchronized(lock) {
                    if (entry === created) expire()
                }
            }
            created
        }
        current.expiryJob?.start()
        return evaluate(current)
    }

    suspend fun retry(): AssessmentOutcome {
        val current = synchronized(lock) {
            if (!_gradingState.value.canRetry || _gradingState.value.isGrading) return unavailable()
            entry ?: return unavailable()
        }
        return evaluate(current)
    }

    fun clear() {
        synchronized(lock) { clearEntry() }
    }

    private suspend fun evaluate(current: GradingEntry): AssessmentOutcome {
        val input = current.evidence
        val job = synchronized(lock) {
            if (entry !== current || _gradingState.value.isGrading) return unavailable()
            try { ensureOwner(input) }
            catch (stale: AssessmentOwnershipException) {
                clearEntry()
                throw stale
            }
            if (isExpired(current)) {
                expire()
                return unavailable()
            }
            _gradingState.value = AssessmentGradingState(isGrading = true)
            applicationScope.async(start = CoroutineStart.LAZY) {
                try {
                    assessmentRepository.evaluate(input)
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (stale: AssessmentOwnershipException) { throw stale }
                catch (_: Exception) { AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.EVALUATION_FAILED) }
            }.also { current.gradingJob = it }
        }
        job.start()
        try {
            val outcome = job.await()
            val pending = synchronized(lock) {
                if (entry !== current) return unavailable()
                ensureOwner(input)
                if (isExpired(current)) {
                    expire()
                    return unavailable()
                }
                val reason = (outcome as? AssessmentOutcome.InsufficientEvidence)?.reason
                val canRetry = reason?.let(PracticeCompletionRules::canRetryGrading) == true && input.hasUserSpeech &&
                    input.callOutcome == CallOutcome.COMPLETED && input.failure == null && input.assessmentAudioId != null
                if (!canRetry) startCleanup(current)
                canRetry to current.cleanupJob
            }
            pending.second?.join()
            return synchronized(lock) {
                if (entry !== current) return@synchronized unavailable()
                ensureOwner(input)
                if (isExpired(current)) {
                    expire()
                    return@synchronized unavailable()
                }
                if (!pending.first) clearEntry()
                _gradingState.value = AssessmentGradingState(outcome = outcome, canRetry = pending.first)
                outcome
            }
        } catch (cancelled: CancellationException) {
            job.cancel()
            synchronized(lock) { if (entry === current) clearEntry() }
            throw cancelled
        } catch (failure: Exception) {
            synchronized(lock) { if (entry === current) clearEntry() }
            throw failure
        } finally {
            synchronized(lock) { if (current.gradingJob === job) current.gradingJob = null }
        }
    }

    private fun clearEntry() {
        val previous = entry
        entry = null
        previous?.expiryJob?.cancel()
        previous?.gradingJob?.cancel()
        _gradingState.value = AssessmentGradingState()
        if (previous != null) startCleanup(previous)
    }

    private fun startCleanup(current: GradingEntry) {
        if (current.cleanupJob != null) return
        current.cleanupJob = current.evidence.assessmentAudioId?.let(::clearAudio)
    }

    private fun ensureOwner(input: AssessmentEvidence) {
        val uid = try { authRepository.requireCurrentUserId() } catch (_: IllegalStateException) { null }
        if (uid == null || input.userId != uid) throw AssessmentOwnershipException()
    }

    private fun isExpired(current: GradingEntry): Boolean =
        PracticeCompletionRules.isEvidenceExpired(current.createdAtMillis, clock.millis()) ||
            current.evidence.expiresAtMillis?.let { clock.millis() >= it } == true

    private fun expire() {
        clearEntry()
        _gradingState.value = AssessmentGradingState(outcome = unavailable(), isExpired = true)
    }

    private fun unavailable() = AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.AUDIO_UNAVAILABLE)

    private fun clearAudio(audioId: String): Job =
        applicationScope.launch {
            try { assessmentRepository.clearAudioEvidence(audioId) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {}
        }

    private class GradingEntry(val evidence: AssessmentEvidence, val createdAtMillis: Long) {
        var expiryJob: Job? = null
        var gradingJob: Deferred<AssessmentOutcome>? = null
        var cleanupJob: Job? = null
    }
}
