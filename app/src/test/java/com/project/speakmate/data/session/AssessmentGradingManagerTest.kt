package com.project.speakmate.data.session

import com.project.speakmate.domain.model.AssessmentOutcome
import com.project.speakmate.domain.model.AssessmentOwnershipException
import com.project.speakmate.domain.model.InsufficientEvidenceReason
import com.project.speakmate.domain.repository.SpeakingAssessmentRepository
import com.project.speakmate.domain.rules.AssessmentTestFixtures
import com.project.speakmate.test.FakeAuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.Clock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AssessmentGradingManagerTest {
    companion object {
        private const val SECOND_AUDIO_ID = "second-audio"
        private const val USER_ID = "user-1"
        private const val DEADLINE_MILLIS = 10_000L
    }

    private val repository = mockk<SpeakingAssessmentRepository>()
    private val clock = mockk<Clock>()
    private val evidence =
        AssessmentTestFixtures.getEvidence().copy(userId = USER_ID, expiresAtMillis = DEADLINE_MILLIS)
    private val failure = AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.EVALUATION_FAILED)

    @Test
    fun cancellingTheCallerCancelsGradingAndCleansTheLeaseOnce() = runTest {
        every { clock.millis() } answers { testScheduler.currentTime }
        val grading = CompletableDeferred<AssessmentOutcome>()
        coEvery { repository.evaluate(evidence) } coAnswers { grading.await() }
        coEvery { repository.clearAudioEvidence(any()) } returns Unit
        val manager =
            AssessmentGradingManager(
                applicationScope = backgroundScope,
                assessmentRepository = repository,
                authRepository = FakeAuthRepository(),
                clock = clock
            )
        val request = backgroundScope.async { manager.grade(evidence) }
        runCurrent()
        assertTrue(manager.gradingState.value.isGrading)

        request.cancelAndJoin()
        runCurrent()
        manager.clear()
        runCurrent()

        assertTrue(request.isCancelled)
        assertFalse(manager.gradingState.value.isGrading)
        assertFalse(manager.gradingState.value.canRetry)
        coVerify(exactly = 1) { repository.evaluate(evidence) }
        coVerify(exactly = 1) { repository.clearAudioEvidence(checkNotNull(evidence.assessmentAudioId)) }
    }

    @Test
    fun aNonRetryableOutcomeWaitsForCleanupBeforeReturning() = runTest {
        every { clock.millis() } answers { testScheduler.currentTime }
        val cleanup = CompletableDeferred<Unit>()
        val outcome = AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.AUDIO_UNAVAILABLE)
        coEvery { repository.evaluate(evidence) } returns outcome
        coEvery { repository.clearAudioEvidence(any()) } coAnswers { cleanup.await() }
        val manager =
            AssessmentGradingManager(
                applicationScope = backgroundScope,
                assessmentRepository = repository,
                authRepository = FakeAuthRepository(),
                clock = clock
            )
        val request = backgroundScope.async { manager.grade(evidence) }
        runCurrent()
        assertFalse(request.isCompleted)
        assertTrue(manager.gradingState.value.isGrading)

        cleanup.complete(Unit)
        runCurrent()

        assertEquals(outcome, request.await())
        assertFalse(manager.gradingState.value.isGrading)
        assertFalse(manager.gradingState.value.canRetry)
        manager.clear()
        manager.clear()
        runCurrent()
        coVerify(exactly = 1) { repository.clearAudioEvidence(checkNotNull(evidence.assessmentAudioId)) }
    }

    @Test
    fun theSavedEvidenceDeadlineExpiresBeforeTheDefaultRetentionWindow() = runTest {
        every { clock.millis() } answers { testScheduler.currentTime }
        coEvery { repository.evaluate(any()) } returns failure
        coEvery { repository.clearAudioEvidence(any()) } returns Unit
        val manager =
            AssessmentGradingManager(
                applicationScope = backgroundScope,
                assessmentRepository = repository,
                authRepository = FakeAuthRepository(),
                clock = clock
            )
        val expiring = evidence.copy(expiresAtMillis = 1L)
        assertEquals(failure, manager.grade(expiring))
        assertTrue(manager.gradingState.value.canRetry)
        runCurrent()
        assertFalse(manager.gradingState.value.isExpired)

        advanceTimeBy(1L)
        runCurrent()

        assertTrue(manager.gradingState.value.isExpired)
        assertFalse(manager.gradingState.value.canRetry)
        manager.clear()
        runCurrent()
        coVerify(exactly = 1) { repository.evaluate(expiring) }
        coVerify(exactly = 1) { repository.clearAudioEvidence(checkNotNull(expiring.assessmentAudioId)) }
    }

    @Test
    fun staleCompletionCannotUpdateTheReplacementLease() = runTest {
        every { clock.millis() } answers { testScheduler.currentTime }
        val stale = CompletableDeferred<AssessmentOutcome>()
        val next = evidence.copy(assessmentAudioId = SECOND_AUDIO_ID)
        coEvery { repository.evaluate(evidence) } coAnswers { withContext(NonCancellable) { stale.await() } }
        coEvery { repository.evaluate(next) } returns failure
        coEvery { repository.clearAudioEvidence(any()) } returns Unit
        val manager =
            AssessmentGradingManager(
                applicationScope = backgroundScope,
                assessmentRepository = repository,
                authRepository = FakeAuthRepository(),
                clock = clock
            )
        val original = backgroundScope.async { manager.grade(evidence) }
        runCurrent()
        assertEquals(failure, manager.grade(next))
        val state = manager.gradingState.value
        stale.complete(AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.AUDIO_UNAVAILABLE))
        runCurrent()
        assertTrue(original.isCancelled)
        assertEquals(state, manager.gradingState.value)
        manager.clear()
        runCurrent()
        coVerify(exactly = 1) { repository.clearAudioEvidence(checkNotNull(evidence.assessmentAudioId)) }
        coVerify(exactly = 1) { repository.clearAudioEvidence(SECOND_AUDIO_ID) }
    }

    @Test
    fun retryUsesTheSameEvidenceAndClearIsRepeatSafe() = runTest {
        every { clock.millis() } answers { testScheduler.currentTime }
        coEvery { repository.evaluate(any()) } returns failure
        coEvery { repository.clearAudioEvidence(any()) } returns Unit
        val manager =
            AssessmentGradingManager(
                applicationScope = backgroundScope,
                assessmentRepository = repository,
                authRepository = FakeAuthRepository(),
                clock = clock
            )
        assertEquals(failure, manager.grade(evidence))
        assertTrue(manager.gradingState.value.canRetry)
        assertEquals(failure, manager.retry())
        manager.clear()
        manager.clear()
        runCurrent()
        assertFalse(manager.gradingState.value.canRetry)
        coVerify(exactly = 2) { repository.evaluate(evidence) }
        coVerify(exactly = 1) { repository.clearAudioEvidence(checkNotNull(evidence.assessmentAudioId)) }
    }

    @Test
    fun expiryCancelsGradingAndReleasesAudioOnce() = runTest {
        every { clock.millis() } answers { testScheduler.currentTime }
        val gate = CompletableDeferred<AssessmentOutcome>()
        coEvery { repository.evaluate(any()) } coAnswers { gate.await() }
        coEvery { repository.clearAudioEvidence(any()) } returns Unit
        val manager =
            AssessmentGradingManager(
                applicationScope = backgroundScope,
                assessmentRepository = repository,
                authRepository = FakeAuthRepository(),
                clock = clock
            )
        val request = backgroundScope.async { manager.grade(evidence) }
        runCurrent()
        assertTrue(manager.gradingState.value.isGrading)
        advanceTimeBy(DEADLINE_MILLIS)
        runCurrent()
        assertTrue(manager.gradingState.value.isExpired)
        assertTrue(request.isCancelled)
        manager.clear()
        runCurrent()
        coVerify(exactly = 1) { repository.clearAudioEvidence(checkNotNull(evidence.assessmentAudioId)) }
    }

    @Test
    fun changingOwnerRejectsRetryAndReleasesTheOldLease() = runTest {
        every { clock.millis() } answers { testScheduler.currentTime }
        coEvery { repository.evaluate(any()) } returns failure
        coEvery { repository.clearAudioEvidence(any()) } returns Unit
        val auth = FakeAuthRepository()
        val manager =
            AssessmentGradingManager(
                applicationScope = backgroundScope,
                assessmentRepository = repository,
                authRepository = auth,
                clock = clock
            )
        manager.grade(evidence)
        auth.signedInUserId.value = null
        val failed =
            try {
                manager.retry()
                false
            } catch (_: AssessmentOwnershipException) {
                true
            }
        assertTrue(failed)
        manager.clear()
        runCurrent()
        coVerify(exactly = 1) { repository.evaluate(any()) }
    }
}
