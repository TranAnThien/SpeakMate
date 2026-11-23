package com.project.speakmate.data.session

import com.project.speakmate.data.audio.AssessmentAudioFileProvider
import com.project.speakmate.domain.model.AssessmentOutcome
import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.CallOutcome
import com.project.speakmate.domain.model.InsufficientEvidenceReason
import com.project.speakmate.domain.model.PracticeCompletionState
import com.project.speakmate.domain.model.PracticeCompletionStatus
import com.project.speakmate.domain.model.PracticeSaveException
import com.project.speakmate.domain.model.PracticeSaveFailure
import com.project.speakmate.domain.model.PracticeType
import com.project.speakmate.domain.model.SpeakingCriteriaResult
import com.project.speakmate.domain.model.TrainingProgress
import com.project.speakmate.domain.model.TrainingStage
import com.project.speakmate.domain.repository.SpeakingAssessmentRepository
import com.project.speakmate.domain.rules.PracticeCompletionRules
import com.project.speakmate.domain.rules.TrainingProgressTestFixtures
import com.project.speakmate.test.FakeAuthRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.time.Clock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeCompletionManagerTest {
    private val clock = Clock.systemUTC()

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun gradingPublishesSavedOnlyAfterTheWriterConfirms() = runTest {
        val history = FakeHistoryRepository()
        history.gate = CompletableDeferred()
        val assessment = FakeSpeakingAssessmentRepository()
        val manager = PracticeCompletionManager(backgroundScope, FakeAuthRepository(), history, assessment, clock)
        val id = requireNotNull(manager.completePractice(PracticeCompletionTestFixtures.practice()))
        runCurrent()
        assertTrue(manager.observeCompletion(id).first() is PracticeCompletionState.Saving)

        history.gate?.complete(Unit)
        runCurrent()
        val saved = manager.observeCompletion(id).first() as PracticeCompletionState.Saved
        assertEquals(AssessmentResult.PASS, saved.completion.attempt.result)
        assertEquals(PracticeCompletionTestFixtures.FEEDBACK, saved.completion.attempt.feedbackSummary)
        assertEquals(PracticeCompletionTestFixtures.training.requiredActions, assessment.evidence.single().requiredActions)
        assertEquals(PracticeCompletionTestFixtures.training.targetLevel, assessment.evidence.single().targetLevel)
    }

    @Test
    fun failedSaveRetriesTheSameAttemptWithoutCallingAiAgain() = runTest {
        val history = FakeHistoryRepository()
        history.failure = IllegalStateException()
        val assessment = FakeSpeakingAssessmentRepository()
        val manager = PracticeCompletionManager(backgroundScope, FakeAuthRepository(), history, assessment, clock)
        val id = requireNotNull(manager.completePractice(PracticeCompletionTestFixtures.practice()))
        runCurrent()
        val failed = manager.observeCompletion(id).first() as PracticeCompletionState.SaveFailed
        assertEquals(AssessmentResult.PASS, failed.completion.attempt.result)

        history.failure = null
        history.gate = CompletableDeferred()
        manager.retrySave(id)
        manager.retrySave(id)
        runCurrent()
        assertTrue((manager.observeCompletion(id).first() as PracticeCompletionState.SaveFailed).isSaving)
        history.gate?.complete(Unit)
        runCurrent()

        assertTrue(manager.observeCompletion(id).first() is PracticeCompletionState.Saved)
        assertEquals(listOf(id, id), history.savedIds)
        assertEquals(1, assessment.evidence.size)
        assertEquals(1, history.attempts.size)
    }

    @Test
    fun leavingDuringGradingDoesNotCancelTheCompletion() = runTest {
        val history = FakeHistoryRepository()
        val assessment = FakeSpeakingAssessmentRepository()
        assessment.gate = CompletableDeferred()
        val manager = PracticeCompletionManager(backgroundScope, FakeAuthRepository(), history, assessment, clock)
        val id = requireNotNull(manager.completePractice(PracticeCompletionTestFixtures.practice()))
        val observed = manager.observeCompletion(id)
        val consumer = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { observed.collect {} }
        runCurrent()
        assertEquals(PracticeCompletionState.Grading, observed.first())
        consumer.cancel()
        manager.releaseCompletion(id)
        runCurrent()
        assertTrue(history.attempts.isEmpty())
        assertEquals(0, assessment.cancelledEvaluations)
        assessment.gate?.complete(Unit)
        runCurrent()

        assertTrue(observed.first() is PracticeCompletionState.Saved)
        assertEquals(1, history.attempts.size)
        assertTrue(id in history.attempts)
        assertEquals(PracticeCompletionState.Unavailable, manager.observeCompletion(id).first())
        assertEquals(listOf(PracticeCompletionTestFixtures.AUDIO_ID), assessment.clearedAudioIds)
    }

    @Test
    fun aSavedEntryIsReleasedWhenItsResultConsumerFinishes() = runTest {
        val manager = PracticeCompletionManager(backgroundScope, FakeAuthRepository(), FakeHistoryRepository(), FakeSpeakingAssessmentRepository(), clock)
        val id = requireNotNull(manager.completePractice(PracticeCompletionTestFixtures.practice()))
        runCurrent()
        assertTrue(manager.observeCompletion(id).first() is PracticeCompletionState.Saved)
        manager.releaseCompletion(id)
        assertEquals(PracticeCompletionState.Unavailable, manager.observeCompletion(id).first())
    }

    @Test
    fun releaseBeforeTheWriterConfirmsDoesNotCancelTheSave() = runTest {
        val history = FakeHistoryRepository()
        history.gate = CompletableDeferred()
        val manager = PracticeCompletionManager(backgroundScope, FakeAuthRepository(), history, FakeSpeakingAssessmentRepository(), clock)
        val id = requireNotNull(manager.completePractice(PracticeCompletionTestFixtures.practice()))
        val observed = manager.observeCompletion(id)
        runCurrent()

        manager.releaseCompletion(id)
        manager.releaseCompletion(id)
        assertTrue(observed.first() is PracticeCompletionState.Saving)
        assertTrue(history.attempts.isEmpty())
        history.gate?.complete(Unit)
        runCurrent()

        assertTrue(observed.first() is PracticeCompletionState.Saved)
        assertEquals(listOf(id), history.savedIds)
        assertTrue(id in history.attempts)
        assertEquals(PracticeCompletionState.Unavailable, manager.observeCompletion(id).first())
    }

    @Test
    fun failedSaveWithoutAConsumerRemainsUntilSignOut() = runTest {
        val auth = FakeAuthRepository()
        val history = FakeHistoryRepository()
        history.failure = IllegalStateException()
        val manager = PracticeCompletionManager(backgroundScope, auth, history, FakeSpeakingAssessmentRepository(), clock)
        val id = requireNotNull(manager.completePractice(PracticeCompletionTestFixtures.practice()))
        runCurrent()
        val failed = manager.observeCompletion(id).first() as PracticeCompletionState.SaveFailed

        manager.releaseCompletion(id)
        runCurrent()
        assertEquals(failed, manager.observeCompletion(id).first())
        assertEquals(AssessmentResult.PASS, failed.completion.attempt.result)
        assertEquals(listOf(id), history.savedIds)
        auth.signedInUserId.value = null
        runCurrent()

        assertEquals(PracticeCompletionState.Unavailable, manager.observeCompletion(id).first())
        manager.retrySave(id)
        runCurrent()
        assertEquals(listOf(id), history.savedIds)
    }

    @Test
    fun signingOutCancelsGradingAndRemovesSavedFailedAndPendingEntries() = runTest {
        val auth = FakeAuthRepository()
        val history = FakeHistoryRepository()
        val assessment = FakeSpeakingAssessmentRepository()
        val manager = PracticeCompletionManager(backgroundScope, auth, history, assessment, clock)
        val practice = PracticeCompletionTestFixtures.practice()
        val savedId = requireNotNull(manager.completePractice(practice))
        runCurrent()
        history.failure = IllegalStateException()
        val failedPractice = practice.copy(session = practice.session.copy(startedAtEpochMillis = PracticeCompletionTestFixtures.EPOCH_START_MILLIS + 1))
        val failedId = requireNotNull(manager.completePractice(failedPractice))
        runCurrent()
        history.failure = null
        assessment.gate = CompletableDeferred()
        val pendingPractice = practice.copy(session = practice.session.copy(startedAtEpochMillis = PracticeCompletionTestFixtures.EPOCH_START_MILLIS + 2))
        val pendingId = requireNotNull(manager.completePractice(pendingPractice))
        runCurrent()
        val observed = listOf(savedId, failedId, pendingId).map(manager::observeCompletion)
        assertTrue(observed[0].first() is PracticeCompletionState.Saved)
        assertTrue(observed[1].first() is PracticeCompletionState.SaveFailed)
        assertEquals(PracticeCompletionState.Grading, observed[2].first())

        auth.signedInUserId.value = null
        runCurrent()
        assertEquals(1, assessment.cancelledEvaluations)
        assertEquals(3, assessment.clearedAudioIds.size)
        observed.forEach { assertEquals(PracticeCompletionState.Unavailable, it.first()) }
        listOf(savedId, failedId, pendingId).forEach {
            assertEquals(PracticeCompletionState.Unavailable, manager.observeCompletion(it).first())
        }
        assessment.gate?.complete(Unit)
        manager.retrySave(failedId)
        runCurrent()
        assertEquals(listOf(savedId, failedId), history.savedIds)
        assertEquals(setOf(savedId), history.attempts.keys)
    }

    @Test
    fun signingOutClearsPendingEntriesAndTheirAudio() = runTest {
        val auth = FakeAuthRepository()
        val history = FakeHistoryRepository()
        val assessment = FakeSpeakingAssessmentRepository()
        assessment.gate = CompletableDeferred()
        val manager = PracticeCompletionManager(backgroundScope, auth, history, assessment, clock)
        val id = requireNotNull(manager.completePractice(PracticeCompletionTestFixtures.practice()))
        runCurrent()
        auth.signedInUserId.value = null
        runCurrent()

        assertEquals(PracticeCompletionState.Unavailable, manager.observeCompletion(id).first())
        assertTrue(history.attempts.isEmpty())
        assertEquals(listOf(PracticeCompletionTestFixtures.AUDIO_ID), assessment.clearedAudioIds)
    }

    @Test
    fun freePracticeSkipsAiAndKeepsFeedbackNull() = runTest {
        val history = FakeHistoryRepository()
        val assessment = FakeSpeakingAssessmentRepository()
        val manager = PracticeCompletionManager(backgroundScope, FakeAuthRepository(), history, assessment, clock)
        val id = requireNotNull(manager.completePractice(PracticeCompletionTestFixtures.practice(isFreePractice = true)))
        runCurrent()

        val attempt = requireNotNull(history.attempts[id])
        assertTrue(assessment.evidence.isEmpty())
        assertNull(attempt.result)
        assertNull(attempt.feedbackSummary)
    }

    @Test
    fun missingEvidenceIsTerminalButTechnicalFailureWaitsForTheUser() = runTest {
        val history = FakeHistoryRepository()
        val assessment = FakeSpeakingAssessmentRepository()
        assessment.outcome = AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.NOT_ENOUGH_SPEECH)
        val manager = PracticeCompletionManager(backgroundScope, FakeAuthRepository(), history, assessment, clock)
        val first = requireNotNull(manager.completePractice(PracticeCompletionTestFixtures.practice()))
        runCurrent()
        assertNull(history.attempts[first]?.result)

        assessment.failure = IllegalStateException()
        val practice = PracticeCompletionTestFixtures.practice()
        val second = requireNotNull(manager.completePractice(practice.copy(session = practice.session
            .copy(startedAtEpochMillis = PracticeCompletionTestFixtures.EPOCH_START_MILLIS + 1))))
        runCurrent()
        assertTrue(manager.observeCompletion(second).first() is PracticeCompletionState.AwaitingGrading)
        assertEquals(setOf(first), history.attempts.keys)
        assertEquals(PracticeCompletionStatus.AWAITING_GRADING, manager.observePendingCompletions().first().single().status)
    }

    @Test
    fun incompleteCallsNeverReachTheWriter() = runTest {
        val history = FakeHistoryRepository()
        val manager = PracticeCompletionManager(backgroundScope, FakeAuthRepository(), history, FakeSpeakingAssessmentRepository(), clock)
        val practice = PracticeCompletionTestFixtures.practice()

        assertNull(manager.completePractice(practice.copy(session = practice.session.copy(outcome = CallOutcome.CANCELLED))))
        assertNull(manager.completePractice(practice.copy(session = practice.session.copy(hasUserSpeech = false))))
        assertTrue(history.savedIds.isEmpty())
    }

    @Test
    fun passAdvancesTheCurrentUnitOnlyOnceWhenTheCommitAcknowledgementIsLost() = runTest {
        val history = FakeHistoryRepository()
        history.path = TrainingProgressTestFixtures.getPath()
        history.progress = TrainingProgress(0, 0, TrainingStage.UNIT)
        history.shouldFailAfterCommit = true
        val assessment = FakeSpeakingAssessmentRepository()
        val manager = PracticeCompletionManager(backgroundScope, FakeAuthRepository(), history, assessment, clock)
        val id = requireNotNull(manager.completePractice(PracticeCompletionTestFixtures.practice()))
        runCurrent()

        assertEquals(TrainingProgress(0, 1, TrainingStage.UNIT), history.progress)
        assertTrue(manager.observeCompletion(id).first() is PracticeCompletionState.SaveFailed)
        history.shouldFailAfterCommit = false
        manager.retrySave(id)
        runCurrent()

        assertEquals(TrainingProgress(0, 1, TrainingStage.UNIT), history.progress)
        assertEquals(1, history.attempts.size)
        assertEquals(listOf(id, id), history.savedIds)
        assertEquals(1, assessment.evidence.size)
    }

    @Test
    fun replayUsesTheProgressAtSaveTimeAndDoesNotAdvanceIt() = runTest {
        val history = FakeHistoryRepository()
        history.path = TrainingProgressTestFixtures.getPath()
        history.progress = TrainingProgress(0, 0, TrainingStage.UNIT)
        val assessment = FakeSpeakingAssessmentRepository()
        assessment.gate = CompletableDeferred()
        val manager = PracticeCompletionManager(backgroundScope, FakeAuthRepository(), history, assessment, clock)
        val id = requireNotNull(manager.completePractice(PracticeCompletionTestFixtures.practice()))
        runCurrent()
        history.progress = TrainingProgress(0, 1, TrainingStage.UNIT)
        assessment.gate?.complete(Unit)
        runCurrent()

        assertEquals(AssessmentResult.PASS, history.attempts[id]?.result)
        assertEquals(TrainingProgress(0, 1, TrainingStage.UNIT), history.progress)
    }

    @Test
    fun partialFailAndNullKeepTheCurrentProgress() = runTest {
        val outcomes = listOf(
            PracticeCompletionTestFixtures.evaluated(SpeakingCriteriaResult(false, false, true, true, true)),
            PracticeCompletionTestFixtures.evaluated(isActionCompleted = false),
            AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.AUDIO_UNAVAILABLE)
        )
        val expected = listOf(AssessmentResult.PARTIAL, AssessmentResult.FAIL, null)
        outcomes.forEachIndexed { index, outcome ->
            val history = FakeHistoryRepository()
            history.path = TrainingProgressTestFixtures.getPath()
            val progress = TrainingProgress(0, 0, TrainingStage.UNIT)
            history.progress = progress
            val assessment = FakeSpeakingAssessmentRepository()
            assessment.outcome = outcome
            val manager = PracticeCompletionManager(backgroundScope, FakeAuthRepository(), history, assessment, clock)
            val id = requireNotNull(manager.completePractice(PracticeCompletionTestFixtures.practice()))
            runCurrent()

            assertEquals(expected[index], history.attempts[id]?.result)
            assertEquals(progress, history.progress)
        }
    }
    @Test
    fun currentGoalPassSavesItsAttemptAndOpensTheNextGoal() = runTest {
        val history = history(TrainingProgress(0, 0, TrainingStage.GOAL_ASSESSMENT))
        val assessment = FakeSpeakingAssessmentRepository()
        val manager = PracticeCompletionManager(backgroundScope, FakeAuthRepository(), history, assessment, clock)
        val id = checkNotNull(manager.completePractice(GoalPracticeCompletionTestFixtures.practice()))
        runCurrent()
        assertEquals(TrainingProgress(1, 0, TrainingStage.UNIT), history.progress)
        val attempt = history.attempts.getValue(id)
        assertEquals(PracticeType.GOAL_ASSESSMENT, attempt.practiceType)
        assertEquals(AssessmentResult.PASS, attempt.result)
        assertEquals(PracticeCompletionTestFixtures.FEEDBACK, attempt.feedbackSummary)
        assertEquals(GoalPracticeCompletionTestFixtures.path.targetLevel, assessment.evidence.single().targetLevel)
        assertEquals(PracticeCompletionTestFixtures.training.requiredActions, assessment.evidence.single().requiredActions)
    }

    @Test
    fun lastGoalPassCompletesThePath() = runTest {
        val history = history(TrainingProgress(1, 0, TrainingStage.GOAL_ASSESSMENT))
        val manager = PracticeCompletionManager(backgroundScope, FakeAuthRepository(), history, FakeSpeakingAssessmentRepository(), clock)
        checkNotNull(manager.completePractice(GoalPracticeCompletionTestFixtures.practice(1)))
        runCurrent()
        assertEquals(TrainingProgress(1, 0, TrainingStage.PATH_COMPLETED), history.progress)
        assertEquals(1, history.attempts.size)
    }

    @Test
    fun replayPassSavesHistoryWithoutAdvancingOrRollingBackProgress() = runTest {
        val progress = TrainingProgress(1, 0, TrainingStage.UNIT)
        val history = history(progress)
        val manager = PracticeCompletionManager(backgroundScope, FakeAuthRepository(), history, FakeSpeakingAssessmentRepository(), clock)
        checkNotNull(manager.completePractice(GoalPracticeCompletionTestFixtures.practice()))
        runCurrent()
        assertEquals(progress, history.progress)
        assertEquals(1, history.attempts.size)
    }

    @Test
    fun partialRequiredActionFailureAndNullAllSaveWithoutChangingProgress() = runTest {
        val progress = TrainingProgress(0, 0, TrainingStage.GOAL_ASSESSMENT)
        val outcomes = listOf(
            PracticeCompletionTestFixtures.evaluated(SpeakingCriteriaResult(true, true, true, false, false)),
            PracticeCompletionTestFixtures.evaluated(isActionCompleted = false),
            AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.AUDIO_UNAVAILABLE)
        )
        val expected = listOf(AssessmentResult.PARTIAL, AssessmentResult.FAIL, null)
        for ((index, outcome) in outcomes.withIndex()) {
            val history = history(progress)
            val assessment = FakeSpeakingAssessmentRepository().also { it.outcome = outcome }
            val manager = PracticeCompletionManager(backgroundScope, FakeAuthRepository(), history, assessment, clock)
            val id = checkNotNull(manager.completePractice(GoalPracticeCompletionTestFixtures.practice()))
            runCurrent()
            assertEquals(progress, history.progress)
            assertEquals(expected[index], history.attempts.getValue(id).result)
            assertTrue(manager.observeCompletion(id).first() is PracticeCompletionState.Saved)
        }
    }

    @Test
    fun retryAfterAnAcknowledgementIsLostDoesNotAdvanceTwice() = runTest {
        val history = history(TrainingProgress(0, 0, TrainingStage.GOAL_ASSESSMENT))
        history.shouldFailAfterCommit = true
        val assessment = FakeSpeakingAssessmentRepository()
        val manager = PracticeCompletionManager(backgroundScope, FakeAuthRepository(), history, assessment, clock)
        val id = checkNotNull(manager.completePractice(GoalPracticeCompletionTestFixtures.practice()))
        runCurrent()
        assertTrue(manager.observeCompletion(id).first() is PracticeCompletionState.SaveFailed)
        history.shouldFailAfterCommit = false
        manager.retrySave(id)
        runCurrent()
        assertEquals(listOf(id, id), history.savedIds)
        assertEquals(1, history.attempts.size)
        assertEquals(1, assessment.evidence.size)
        assertEquals(TrainingProgress(1, 0, TrainingStage.UNIT), history.progress)
        assertTrue(manager.observeCompletion(id).first() is PracticeCompletionState.Saved)
    }

    @Test
    fun leavingGoalGradingStillSavesAndReleasesTheResult() = runTest {
        val history = history(TrainingProgress(0, 0, TrainingStage.GOAL_ASSESSMENT))
        val assessment = FakeSpeakingAssessmentRepository().also { it.gate = CompletableDeferred() }
        val manager = PracticeCompletionManager(backgroundScope, FakeAuthRepository(), history, assessment, clock)
        val id = checkNotNull(manager.completePractice(GoalPracticeCompletionTestFixtures.practice()))
        runCurrent()
        manager.releaseCompletion(id)
        assessment.gate?.complete(Unit)
        runCurrent()
        assertTrue(id in history.attempts)
        assertEquals(TrainingProgress(1, 0, TrainingStage.UNIT), history.progress)
        assertEquals(PracticeCompletionState.Unavailable, manager.observeCompletion(id).first())
        assertEquals(listOf(PracticeCompletionTestFixtures.AUDIO_ID), assessment.clearedAudioIds)
    }

    @Test
    fun pendingListTracksGradingSavingFailureAndRetryWithoutAResultConsumer() = runTest {
        val history = FakeHistoryRepository().also { it.failure = IllegalStateException(); it.gate = CompletableDeferred() }
        val assessment = FakeSpeakingAssessmentRepository().also { it.gate = CompletableDeferred() }
        val manager = PracticeCompletionManager(backgroundScope, FakeAuthRepository(), history, assessment, clock)
        val pending = manager.observePendingCompletions()
        val id = requireNotNull(manager.completePractice(PracticeCompletionTestFixtures.practice()))
        runCurrent()
        val summary = pending.first().single()
        assertEquals(id, summary.attemptId)
        assertEquals(PracticeType.TRAINING_UNIT, summary.practiceType)
        assertEquals(PracticeCompletionTestFixtures.scenario.name, summary.scenarioTitle)
        assertEquals(PracticeCompletionTestFixtures.EPOCH_START_MILLIS, summary.startedAtEpochMillis)
        assertEquals(PracticeCompletionStatus.GRADING, summary.status)
        manager.releaseCompletion(id)
        assessment.gate?.complete(Unit)
        runCurrent()
        assertEquals(PracticeCompletionStatus.SAVING, pending.first().single().status)
        history.gate?.complete(Unit)
        runCurrent()
        assertEquals(PracticeCompletionStatus.SAVE_FAILED, pending.first().single().status)

        history.failure = null
        history.gate = CompletableDeferred()
        manager.retrySave(id)
        manager.retrySave(id)
        runCurrent()
        assertEquals(PracticeCompletionStatus.SAVING, pending.first().single().status)
        history.gate?.complete(Unit)
        runCurrent()
        assertTrue(pending.first().isEmpty())
        assertEquals(listOf(id, id), history.savedIds)
        assertEquals(1, history.attempts.size)
        assertEquals(1, assessment.evidence.size)
        assertEquals(PracticeCompletionState.Unavailable, manager.observeCompletion(id).first())
    }

    @Test
    fun pendingListContainsAllPracticeTypesButNeverSavedOrIncompleteCalls() = runTest {
        val history = FakeHistoryRepository().also { it.failure = IllegalStateException() }
        val assessment = FakeSpeakingAssessmentRepository()
        val manager = PracticeCompletionManager(backgroundScope, FakeAuthRepository(), history, assessment, clock)
        val practices = listOf(
            PracticeCompletionTestFixtures.practice(isFreePractice = true),
            PracticeCompletionTestFixtures.practice(),
            GoalPracticeCompletionTestFixtures.practice()
        )
        val ids = practices.map { requireNotNull(manager.completePractice(it)) }
        runCurrent()
        val pending = manager.observePendingCompletions().first()
        assertEquals(ids.toSet(), pending.map { it.attemptId }.toSet())
        assertEquals(PracticeType.entries.toSet(), pending.map { it.practiceType }.toSet())
        assertTrue(pending.all { it.status == PracticeCompletionStatus.SAVE_FAILED })
        val incomplete = practices.first().let { it.copy(session = it.session.copy(hasUserSpeech = false)) }
        assertNull(manager.completePractice(incomplete))
        assertEquals(3, manager.observePendingCompletions().first().size)
        history.failure = null
        ids.forEach(manager::retrySave)
        runCurrent()
        assertTrue(manager.observePendingCompletions().first().isEmpty())
        ids.forEach { assertTrue(manager.observeCompletion(it).first() is PracticeCompletionState.Saved) }
    }

    @Test
    fun switchingUsersClearsTheListAndCannotRetryAnotherUsersFailedSave() = runTest {
        val auth = FakeAuthRepository()
        val history = FakeHistoryRepository().also { it.failure = IllegalStateException() }
        val manager = PracticeCompletionManager(backgroundScope, auth, history, FakeSpeakingAssessmentRepository(), clock)
        val oldId = requireNotNull(manager.completePractice(PracticeCompletionTestFixtures.practice(isFreePractice = true)))
        runCurrent()
        assertEquals(oldId, manager.observePendingCompletions().first().single().attemptId)
        auth.signedInUserId.value = "user-2"
        assertTrue(manager.observePendingCompletions().first().isEmpty())
        manager.retrySave(oldId)
        runCurrent()
        val newId = requireNotNull(manager.completePractice(PracticeCompletionTestFixtures.practice(isFreePractice = true)))
        runCurrent()
        assertEquals(listOf(newId), manager.observePendingCompletions().first().map { it.attemptId })
        assertEquals(PracticeCompletionState.Unavailable, manager.observeCompletion(oldId).first())
        auth.signedInUserId.value = null
        runCurrent()
        assertTrue(manager.observePendingCompletions().first().isEmpty())
        assertEquals(listOf(oldId, newId), history.savedIds)
    }

    @Test
    fun lateGradingAfterSignOutCannotRecreateAPendingEntryOrWriteHistory() = runTest {
        val auth = FakeAuthRepository()
        val gate = CompletableDeferred<Unit>()
        val assessment = mockk<SpeakingAssessmentRepository>()
        coEvery { assessment.evaluate(any()) } coAnswers {
            withContext(NonCancellable) { gate.await() }
            PracticeCompletionTestFixtures.evaluated()
        }
        coEvery { assessment.clearAudioEvidence(any()) } returns Unit
        val history = FakeHistoryRepository()
        val manager = PracticeCompletionManager(backgroundScope, auth, history, assessment, clock)
        val id = requireNotNull(manager.completePractice(PracticeCompletionTestFixtures.practice()))
        runCurrent()
        auth.signedInUserId.value = null
        runCurrent()
        gate.complete(Unit)
        runCurrent()
        auth.signedInUserId.value = "user-2"
        runCurrent()
        assertTrue(manager.observePendingCompletions().first().isEmpty())
        assertTrue(history.savedIds.isEmpty())
        assertEquals(PracticeCompletionState.Unavailable, manager.observeCompletion(id).first())
    }

    @Test
    fun nextRecorderCannotDeleteAudioWhileCompletionWaitsToReadIt() = runTest {
        val files = AssessmentAudioFileProvider(folder.root)
        val firstId = files.createAudioId()
        files.openRecorder(firstId).also { it.append(byteArrayOf(1, 2, 3, 4)); it.close() }
        val firstWav = requireNotNull(files.getAudioFile(firstId)).readBytes().toList()
        val gate = CompletableDeferred<Unit>()
        val assessment = mockk<SpeakingAssessmentRepository>()
        coEvery { assessment.evaluate(any()) } coAnswers {
            gate.await()
            assertEquals(firstWav, requireNotNull(files.getAudioFile(firstId)).readBytes().toList())
            PracticeCompletionTestFixtures.evaluated()
        }
        coEvery { assessment.clearAudioEvidence(any()) } answers { files.deleteAudio(firstArg()) }
        val history = FakeHistoryRepository()
        val manager = PracticeCompletionManager(backgroundScope, FakeAuthRepository(), history, assessment, clock)
        val practice = PracticeCompletionTestFixtures.practice()
        val id = requireNotNull(manager.completePractice(practice.copy(session = practice.session.copy(assessmentAudioId = firstId))))
        manager.releaseCompletion(id)
        val secondId = files.createAudioId()
        val recorder = files.openRecorder(secondId)
        try {
            recorder.append(byteArrayOf(5, 6))
            gate.complete(Unit)
            runCurrent()
            assertEquals(AssessmentResult.PASS, history.attempts[id]?.result)
            assertNull(files.getAudioFile(firstId))
            assertTrue(files.getAudioFile(secondId) != null)
            assertTrue(recorder.append(byteArrayOf(7, 8)))
        } finally {
            recorder.close()
        }
    }

    private fun history(progress: TrainingProgress) = FakeHistoryRepository().also {
        it.path = GoalPracticeCompletionTestFixtures.path
        it.progress = progress
    }

    @Test
    fun doubleCompletionAndRetryReuseTheSameOperationAndEvidence() = runTest {
        val history = FakeHistoryRepository()
        val assessment = FakeSpeakingAssessmentRepository().also {
            it.outcome = AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.EVALUATION_FAILED)
        }
        val manager = PracticeCompletionManager(backgroundScope, FakeAuthRepository(), history, assessment, clock)
        val practice = PracticeCompletionTestFixtures.practice()
        val id = requireNotNull(manager.completePractice(practice))
        assertEquals(id, manager.completePractice(practice))
        runCurrent()
        val first = assessment.evidence.single()
        assertEquals(id, first.operationId)
        assertTrue(history.attempts.isEmpty())
        assertTrue(assessment.clearedAudioIds.isEmpty())
        assessment.outcome = PracticeCompletionTestFixtures.evaluated()
        assessment.gate = CompletableDeferred()
        manager.retryGrading(id)
        manager.retryGrading(id)
        manager.saveUnassessed(id)
        runCurrent()
        assertEquals(listOf(first, first), assessment.evidence)
        assertEquals(PracticeCompletionState.Grading, manager.observeCompletion(id).first())
        assertTrue(history.attempts.isEmpty())
        assessment.gate?.complete(Unit)
        runCurrent()
        assertEquals(listOf(id), history.savedIds)
        assertEquals(AssessmentResult.PASS, history.attempts[id]?.result)
        assertEquals(listOf(PracticeCompletionTestFixtures.AUDIO_ID), assessment.clearedAudioIds)
    }

    @Test
    fun choosingUnassessedCommitsOneNullResultWithoutAdvancingOrGradingAgain() = runTest {
        val progress = TrainingProgress(0, 0, TrainingStage.UNIT)
        val history = FakeHistoryRepository().also {
            it.path = TrainingProgressTestFixtures.getPath()
            it.progress = progress
        }
        val assessment = FakeSpeakingAssessmentRepository().also {
            it.outcome = AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.EVALUATION_INVALID)
        }
        val manager = PracticeCompletionManager(backgroundScope, FakeAuthRepository(), history, assessment, clock)
        val id = requireNotNull(manager.completePractice(PracticeCompletionTestFixtures.practice()))
        runCurrent()
        manager.saveUnassessed(id)
        manager.saveUnassessed(id)
        manager.retryGrading(id)
        runCurrent()
        val completion = (manager.observeCompletion(id).first() as PracticeCompletionState.Saved).completion
        assertEquals(AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.EVALUATION_INVALID), completion.outcome)
        assertNull(completion.attempt.result)
        assertNull(completion.attempt.criteriaResult)
        assertTrue(!completion.attempt.feedbackSummary.isNullOrBlank())
        assertEquals(progress, history.progress)
        assertEquals(listOf(id), history.savedIds)
        assertEquals(1, assessment.evidence.size)
        assertEquals(listOf(PracticeCompletionTestFixtures.AUDIO_ID), assessment.clearedAudioIds)
    }

    @Test
    fun audioExpiryCommitsNullWithoutAnAutomaticAiRetry() = runTest {
        val controlledClock = mockk<Clock>()
        every { controlledClock.millis() } answers { testScheduler.currentTime }
        val history = FakeHistoryRepository()
        val assessment = FakeSpeakingAssessmentRepository().also {
            it.outcome = AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.EVALUATION_FAILED)
        }
        val manager = PracticeCompletionManager(backgroundScope, FakeAuthRepository(), history, assessment, controlledClock)
        val id = requireNotNull(manager.completePractice(PracticeCompletionTestFixtures.practice()))
        runCurrent()
        advanceTimeBy(PracticeCompletionRules.AUDIO_RETENTION_MILLIS - 1)
        runCurrent()
        assertTrue(history.attempts.isEmpty())
        advanceTimeBy(1)
        runCurrent()
        val completion = (manager.observeCompletion(id).first() as PracticeCompletionState.Saved).completion
        assertEquals(AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.AUDIO_UNAVAILABLE), completion.outcome)
        assertNull(completion.attempt.result)
        assertEquals(1, assessment.evidence.size)
        assertEquals(listOf(PracticeCompletionTestFixtures.AUDIO_ID), assessment.clearedAudioIds)
    }


    @Test
    fun invalidRequiredActionResponseRemainsPendingInsteadOfCommittingNull() = runTest {
        val history = FakeHistoryRepository()
        val assessment = FakeSpeakingAssessmentRepository().also {
            it.outcome = PracticeCompletionTestFixtures.evaluated().copy(requiredActionResults = emptyMap())
        }
        val manager = PracticeCompletionManager(backgroundScope, FakeAuthRepository(), history, assessment, clock)
        val id = requireNotNull(manager.completePractice(PracticeCompletionTestFixtures.practice()))
        runCurrent()
        assertEquals(InsufficientEvidenceReason.EVALUATION_INVALID,
            (manager.observeCompletion(id).first() as PracticeCompletionState.AwaitingGrading).reason)
        assertTrue(history.savedIds.isEmpty())
        assertTrue(assessment.clearedAudioIds.isEmpty())
    }

    @Test
    fun anotherUserCannotReadASavedCompletionBeforeBackgroundCleanupRuns() = runTest {
        val auth = FakeAuthRepository()
        val history = FakeHistoryRepository()
        val manager = PracticeCompletionManager(backgroundScope, auth, history, FakeSpeakingAssessmentRepository(), clock)
        val id = requireNotNull(manager.completePractice(PracticeCompletionTestFixtures.practice()))
        runCurrent()
        val oldObservation = manager.observeCompletion(id)
        assertTrue(oldObservation.first() is PracticeCompletionState.Saved)

        auth.signedInUserId.value = "user-2"

        assertEquals(PracticeCompletionState.Unavailable, manager.observeCompletion(id).first())
        assertEquals(PracticeCompletionState.Unavailable, oldObservation.first())
        assertEquals(listOf(id), history.savedIds)
    }

    @Test
    fun signOutImmediatelyHidesPendingCompletionFromNewObservers() = runTest {
        val auth = FakeAuthRepository()
        val history = FakeHistoryRepository()
        val assessment = FakeSpeakingAssessmentRepository().also {
            it.outcome = AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.EVALUATION_FAILED)
        }
        val manager = PracticeCompletionManager(backgroundScope, auth, history, assessment, clock)
        val id = requireNotNull(manager.completePractice(PracticeCompletionTestFixtures.practice()))
        runCurrent()
        assertTrue(manager.observeCompletion(id).first() is PracticeCompletionState.AwaitingGrading)

        auth.signedInUserId.value = null

        assertEquals(PracticeCompletionState.Unavailable, manager.observeCompletion(id).first())
        assertTrue(history.savedIds.isEmpty())
    }

    @Test
    fun automaticSaveRetriesUseFiniteBackoffAndNeverRegrade() = runTest {
        val history = FakeHistoryRepository().also { it.failure = PracticeSaveException(PracticeSaveFailure.NETWORK) }
        val assessment = FakeSpeakingAssessmentRepository()
        val manager = PracticeCompletionManager(backgroundScope, FakeAuthRepository(), history, assessment, clock)
        manager.setNetworkAvailable(true)
        val id = requireNotNull(manager.completePractice(PracticeCompletionTestFixtures.practice()))
        runCurrent()
        assertEquals(listOf(id), history.savedIds)
        listOf(2_000L, 8_000L, 30_000L).forEach { delay ->
            val before = history.savedIds.size
            advanceTimeBy(delay - 1)
            runCurrent()
            assertEquals(before, history.savedIds.size)
            advanceTimeBy(1)
            runCurrent()
            assertEquals(before + 1, history.savedIds.size)
        }
        advanceTimeBy(60_000L)
        runCurrent()
        assertEquals(List(4) { id }, history.savedIds)
        assertEquals(1, assessment.evidence.size)
        assertTrue(manager.observeCompletion(id).first() is PracticeCompletionState.SaveFailed)
    }

    @Test
    fun goingOfflineCancelsTheDelayAndReconnectResumesTheSameId() = runTest {
        val history = FakeHistoryRepository().also { it.failure = PracticeSaveException(PracticeSaveFailure.NETWORK) }
        val assessment = FakeSpeakingAssessmentRepository()
        val manager = PracticeCompletionManager(backgroundScope, FakeAuthRepository(), history, assessment, clock)
        manager.setNetworkAvailable(true)
        val id = requireNotNull(manager.completePractice(PracticeCompletionTestFixtures.practice()))
        runCurrent()
        manager.setNetworkAvailable(false)
        advanceTimeBy(60_000L)
        runCurrent()
        assertEquals(listOf(id), history.savedIds)
        history.failure = null
        manager.setNetworkAvailable(true)
        advanceTimeBy(2_000L)
        runCurrent()
        assertEquals(listOf(id, id), history.savedIds)
        assertTrue(manager.observeCompletion(id).first() is PracticeCompletionState.Saved)
        assertEquals(1, assessment.evidence.size)
    }

    @Test
    fun foregroundDoesNotResetExhaustedRetriesAndReconnectKeepsTheSameId() = runTest {
        val history = FakeHistoryRepository().also { it.failure = PracticeSaveException(PracticeSaveFailure.NETWORK) }
        val manager = PracticeCompletionManager(backgroundScope, FakeAuthRepository(), history, FakeSpeakingAssessmentRepository(), clock)
        manager.setNetworkAvailable(true)
        val id = requireNotNull(manager.completePractice(PracticeCompletionTestFixtures.practice()))
        runCurrent()
        listOf(2_000L, 8_000L, 30_000L).forEach { advanceTimeBy(it); runCurrent() }
        assertEquals(4, history.savedIds.size)
        history.failure = null
        manager.resumePendingSaves()
        manager.resumePendingSaves()
        advanceTimeBy(2_000L)
        runCurrent()
        assertEquals(List(4) { id }, history.savedIds)
        manager.setNetworkAvailable(false)
        manager.setNetworkAvailable(true)
        advanceTimeBy(2_000L)
        runCurrent()
        assertEquals(List(5) { id }, history.savedIds)
        assertTrue(manager.observeCompletion(id).first() is PracticeCompletionState.Saved)
    }


    @Test
    fun anAutomaticRetryCannotPublishSavedUntilTheWriterAcknowledges() = runTest {
        val history = FakeHistoryRepository().also { it.failure = PracticeSaveException(PracticeSaveFailure.NETWORK) }
        val manager = PracticeCompletionManager(backgroundScope, FakeAuthRepository(), history, FakeSpeakingAssessmentRepository(), clock)
        manager.setNetworkAvailable(true)
        val id = requireNotNull(manager.completePractice(PracticeCompletionTestFixtures.practice()))
        runCurrent()
        history.failure = null
        history.gate = CompletableDeferred()
        advanceTimeBy(2_000L)
        runCurrent()
        assertTrue((manager.observeCompletion(id).first() as PracticeCompletionState.SaveFailed).isSaving)
        history.gate?.complete(Unit)
        runCurrent()
        assertTrue(manager.observeCompletion(id).first() is PracticeCompletionState.Saved)
    }

    @Test
    fun signOutCancelsAScheduledAutomaticRetry() = runTest {
        val auth = FakeAuthRepository()
        val history = FakeHistoryRepository().also { it.failure = PracticeSaveException(PracticeSaveFailure.NETWORK) }
        val manager = PracticeCompletionManager(backgroundScope, auth, history, FakeSpeakingAssessmentRepository(), clock)
        manager.setNetworkAvailable(true)
        val id = requireNotNull(manager.completePractice(PracticeCompletionTestFixtures.practice()))
        runCurrent()
        auth.signOut()
        runCurrent()
        advanceTimeBy(60_000L)
        runCurrent()
        assertEquals(listOf(id), history.savedIds)
        assertEquals(PracticeCompletionState.Unavailable, manager.observeCompletion(id).first())
        assertTrue(manager.observePendingCompletions().first().isEmpty())
    }

    @Test
    fun timedOutSaveRemainsPendingAndCanReconcileWithTheSameId() = runTest {
        val history = FakeHistoryRepository().also { it.gate = CompletableDeferred() }
        val assessment = FakeSpeakingAssessmentRepository()
        val manager = PracticeCompletionManager(backgroundScope, FakeAuthRepository(), history, assessment, clock)
        val id = requireNotNull(manager.completePractice(PracticeCompletionTestFixtures.practice()))
        runCurrent()
        advanceTimeBy(30_000L)
        runCurrent()
        assertEquals(PracticeSaveFailure.NETWORK, (manager.observeCompletion(id).first() as PracticeCompletionState.SaveFailed).reason)
        assertTrue(history.attempts.isEmpty())
        history.gate?.complete(Unit)
        manager.retrySave(id)
        runCurrent()
        assertEquals(listOf(id, id), history.savedIds)
        assertTrue(manager.observeCompletion(id).first() is PracticeCompletionState.Saved)
        assertEquals(1, assessment.evidence.size)
    }

    @Test
    fun aNewProcessOwnerDoesNotRestoreUnsentCompletionsOrCallAi() = runTest {
        val auth = FakeAuthRepository()
        val history = FakeHistoryRepository().also { it.failure = PracticeSaveException(PracticeSaveFailure.NETWORK) }
        val first = PracticeCompletionManager(backgroundScope, auth, history, FakeSpeakingAssessmentRepository(), clock)
        val id = requireNotNull(first.completePractice(PracticeCompletionTestFixtures.practice()))
        runCurrent()
        val secondAssessment = FakeSpeakingAssessmentRepository()
        val second = PracticeCompletionManager(backgroundScope, auth, history, secondAssessment, clock)
        runCurrent()
        assertTrue(first.observeCompletion(id).first() is PracticeCompletionState.SaveFailed)
        assertEquals(PracticeCompletionState.Unavailable, second.observeCompletion(id).first())
        assertTrue(second.observePendingCompletions().first().isEmpty())
        assertTrue(secondAssessment.evidence.isEmpty())
        assertEquals(listOf(id), history.savedIds)
    }

    @Test
    fun resultInRamDoesNotExpireAfterSevenDays() = runTest {
        val testClock = mockk<Clock>()
        every { testClock.millis() } answers { testScheduler.currentTime }
        val history = FakeHistoryRepository().also { it.failure = PracticeSaveException(PracticeSaveFailure.NETWORK) }
        val assessment = FakeSpeakingAssessmentRepository()
        val manager = PracticeCompletionManager(backgroundScope, FakeAuthRepository(), history, assessment, testClock)
        val id = requireNotNull(manager.completePractice(PracticeCompletionTestFixtures.practice()))
        runCurrent()
        advanceTimeBy(8 * PracticeCompletionRules.AUDIO_RETENTION_MILLIS)
        runCurrent()
        assertTrue(manager.observeCompletion(id).first() is PracticeCompletionState.SaveFailed)
        history.failure = null
        manager.retrySave(id)
        runCurrent()
        assertTrue(manager.observeCompletion(id).first() is PracticeCompletionState.Saved)
        assertEquals(listOf(id, id), history.savedIds)
        assertEquals(1, assessment.evidence.size)
    }

    @Test
    fun gradingRetryUsesTheOriginalAudioDeadline() = runTest {
        val testClock = mockk<Clock>()
        every { testClock.millis() } answers { testScheduler.currentTime }
        val history = FakeHistoryRepository()
        val assessment = FakeSpeakingAssessmentRepository().also { it.outcome = AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.EVALUATION_FAILED) }
        val manager = PracticeCompletionManager(backgroundScope, FakeAuthRepository(), history, assessment, testClock)
        val id = requireNotNull(manager.completePractice(PracticeCompletionTestFixtures.practice()))
        runCurrent()
        val original = (manager.observeCompletion(id).first() as PracticeCompletionState.AwaitingGrading).expiresAtMillis
        advanceTimeBy(PracticeCompletionRules.AUDIO_RETENTION_MILLIS - 1)
        manager.retryGrading(id)
        runCurrent()
        assertEquals(original, (manager.observeCompletion(id).first() as PracticeCompletionState.AwaitingGrading).expiresAtMillis)
        assertEquals(assessment.evidence[0], assessment.evidence[1])
        advanceTimeBy(1)
        runCurrent()
        assertNull((manager.observeCompletion(id).first() as PracticeCompletionState.Saved).completion.attempt.result)
        assertEquals(1, assessment.clearedAudioIds.size)
    }

    @Test
    fun retainedCompletionFlowRejectsAChangedUidBeforeCleanupRuns() = runTest {
        val auth = FakeAuthRepository()
        val manager = PracticeCompletionManager(backgroundScope, auth, FakeHistoryRepository(), FakeSpeakingAssessmentRepository(), clock)
        val id = requireNotNull(manager.completePractice(PracticeCompletionTestFixtures.practice()))
        runCurrent()
        val retained = manager.observeCompletion(id)
        assertTrue(retained.first() is PracticeCompletionState.Saved)
        auth.signOut()
        assertEquals(PracticeCompletionState.Unavailable, retained.first())
    }
}
