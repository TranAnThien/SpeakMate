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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
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
    private val evidence = AssessmentTestFixtures.getEvidence().copy(userId = USER_ID, expiresAtMillis = DEADLINE_MILLIS)
    private val failure = AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.EVALUATION_FAILED)

    @Test
    fun staleCompletionCannotUpdateTheReplacementLease() = runTest {
        every { clock.millis() } answers { testScheduler.currentTime }
        val stale = CompletableDeferred<AssessmentOutcome>()
        val next = evidence.copy(assessmentAudioId = SECOND_AUDIO_ID)
        coEvery { repository.evaluate(evidence) } coAnswers { withContext(NonCancellable) { stale.await() } }
        coEvery { repository.evaluate(next) } returns failure
        coEvery { repository.clearAudioEvidence(any()) } returns Unit
        val manager = AssessmentGradingManager(backgroundScope, repository, FakeAuthRepository(), clock)
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
        val manager = AssessmentGradingManager(backgroundScope, repository, FakeAuthRepository(), clock)
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
        val manager = AssessmentGradingManager(backgroundScope, repository, FakeAuthRepository(), clock)
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
        val manager = AssessmentGradingManager(backgroundScope, repository, auth, clock)
        manager.grade(evidence)
        auth.signedInUserId.value = null
        val failed = try { manager.retry(); false } catch (_: AssessmentOwnershipException) { true }
        assertTrue(failed)
        manager.clear()
        runCurrent()
        coVerify(exactly = 1) { repository.evaluate(any()) }
    }
}
