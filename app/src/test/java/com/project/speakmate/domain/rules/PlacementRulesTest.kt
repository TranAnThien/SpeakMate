package com.project.speakmate.domain.rules

import com.project.speakmate.domain.model.AssessmentOutcome
import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.AssessmentTask
import com.project.speakmate.domain.model.InsufficientEvidenceReason
import com.project.speakmate.domain.model.PlacementCheckpoint
import com.project.speakmate.domain.model.PlacementRole
import com.project.speakmate.domain.model.PlacementStep
import com.project.speakmate.domain.model.PlacementStopReason
import com.project.speakmate.domain.model.PlacementTaskRecord
import com.project.speakmate.domain.model.PlacementTransition
import com.project.speakmate.domain.model.SpeakingLevel
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlacementRulesTest {
    private val tasks = PlacementTestFixtures.getTasks()
    private val initial = PlacementTestFixtures.getCheckpoint()

    @Test
    fun initialA2PassGoesToB1Probe() = assertPass(PlacementStep.INITIAL_A2, PlacementStep.B1_PROBE)

    @Test
    fun initialA2PartialOrFailGoesToA2Confirmation() = assertNotPassed(PlacementStep.INITIAL_A2, PlacementStep.A2_CONFIRMATION)

    @Test
    fun b1ProbePassGoesToB2Probe() = assertPass(PlacementStep.B1_PROBE, PlacementStep.B2_PROBE)

    @Test
    fun b1ProbePartialOrFailGoesToA2Confirmation() = assertNotPassed(PlacementStep.B1_PROBE, PlacementStep.A2_CONFIRMATION)

    @Test
    fun b2ProbePassGoesToB2Confirmation() = assertPass(PlacementStep.B2_PROBE, PlacementStep.B2_CONFIRMATION)

    @Test
    fun b2ProbePartialOrFailGoesToB1Confirmation() = assertNotPassed(PlacementStep.B2_PROBE, PlacementStep.B1_CONFIRMATION)

    @Test
    fun b2ConfirmationPassConfirmsB2() = assertPass(PlacementStep.B2_CONFIRMATION, null)

    @Test
    fun b2ConfirmationPartialOrFailGoesToB1Confirmation() = assertNotPassed(PlacementStep.B2_CONFIRMATION, PlacementStep.B1_CONFIRMATION)

    @Test
    fun b1ConfirmationPassConfirmsB1() = assertPass(PlacementStep.B1_CONFIRMATION, null)

    @Test
    fun b1ConfirmationPartialOrFailGoesToA2Confirmation() = assertNotPassed(PlacementStep.B1_CONFIRMATION, PlacementStep.A2_CONFIRMATION)

    @Test
    fun a2ConfirmationPassConfirmsA2() = assertPass(PlacementStep.A2_CONFIRMATION, null)

    @Test
    fun a2ConfirmationPartialOrFailGoesToA1Confirmation() = assertNotPassed(PlacementStep.A2_CONFIRMATION, PlacementStep.A1_CONFIRMATION)

    @Test
    fun a1ConfirmationPassConfirmsA1() = assertPass(PlacementStep.A1_CONFIRMATION, null)

    @Test
    fun a1ConfirmationPartialOrFailConfirmsA1() = assertNotPassed(PlacementStep.A1_CONFIRMATION, null)

    @Test
    fun allPassConfirmsB2InFourTasks() {
        assertPath(SpeakingLevel.B2, AssessmentResult.PASS, AssessmentResult.PASS, AssessmentResult.PASS, AssessmentResult.PASS)
    }

    @Test
    fun b2ConfirmationFailThenB1PassConfirmsB1() {
        assertPath(SpeakingLevel.B1, AssessmentResult.PASS, AssessmentResult.PASS, AssessmentResult.PASS, AssessmentResult.FAIL, AssessmentResult.PASS)
    }

    @Test
    fun longestPathConfirmsA1InSevenTasks() {
        assertPath(SpeakingLevel.A1, AssessmentResult.PASS, AssessmentResult.PASS, AssessmentResult.PASS, AssessmentResult.FAIL,
            AssessmentResult.FAIL, AssessmentResult.FAIL, AssessmentResult.FAIL)
    }

    @Test
    fun b1ProbeFailConfirmsA2ThroughConfirmation() {
        assertPath(SpeakingLevel.A2, AssessmentResult.PASS, AssessmentResult.FAIL, AssessmentResult.PASS)
    }

    @Test
    fun initialFailThenA2ConfirmationFailConfirmsA1() {
        assertPath(SpeakingLevel.A1, AssessmentResult.FAIL, AssessmentResult.FAIL, AssessmentResult.FAIL)
    }

    @Test
    fun singlePassNeverConfirmsLevel() {
        val next = apply(initial, PlacementTestFixtures.getOutcome(AssessmentResult.PASS)) as PlacementTransition.NextTask
        assertEquals(PlacementStep.B1_PROBE, next.checkpoint.step)
        assertNull(next.checkpoint.pendingResult)
    }

    @Test
    fun everyPathEndsWithinSevenValidTasks() {
        val taskCounts = walkPaths(initial)
        assertTrue(taskCounts.isNotEmpty())
        assertEquals(PlacementRules.MAX_VALID_TASKS, taskCounts.max())
        assertTrue(taskCounts.all { it <= PlacementRules.MAX_VALID_TASKS })
    }

    @Test
    fun onlyConfirmationStepsConfirm() {
        PlacementStep.entries.forEach { step ->
            AssessmentResult.entries.forEach { result ->
                val transition = apply(PlacementTestFixtures.getCheckpoint(step), PlacementTestFixtures.getOutcome(result))
                if (transition is PlacementTransition.Confirmed) assertEquals(PlacementRole.CONFIRMATION, step.role)
            }
        }
    }

    @Test
    fun speakingProfileComesFromConfirmingTask() {
        val outcome = PlacementTestFixtures.getOutcome(AssessmentResult.FAIL)
        val confirmed = apply(PlacementTestFixtures.getCheckpoint(PlacementStep.A1_CONFIRMATION), outcome) as PlacementTransition.Confirmed
        assertEquals(outcome.criteria, confirmed.result.speakingProfile)
        assertEquals(confirmed.result, confirmed.checkpoint.pendingResult)
        assertEquals(SpeakingLevel.A1, confirmed.result.level)
        assertEquals(PlacementTestFixtures.CREATED_AT_MILLIS + 1, confirmed.result.assessedAtMillis)
        assertNull(confirmed.checkpoint.currentTaskId)
    }

    @Test
    fun a2ConfirmationAvoidsInitialA2Scenario() = assertDifferentContext(PlacementStep.INITIAL_A2, PlacementStep.A2_CONFIRMATION)

    @Test
    fun b1ConfirmationAvoidsB1ProbeScenario() = assertDifferentContext(PlacementStep.B1_PROBE, PlacementStep.B1_CONFIRMATION)

    @Test
    fun b2ConfirmationAvoidsB2ProbeScenario() = assertDifferentContext(PlacementStep.B2_PROBE, PlacementStep.B2_CONFIRMATION)

    @Test
    fun confirmationOnlyExcludesScenariosPreviouslyUsedAtSameLevel() {
        val otherLevel = tasks.first { it.targetLevel == SpeakingLevel.B1 }
        val sharedScenario = tasks.first().copy(scenarioId = otherLevel.scenarioId)
        val record = PlacementTaskRecord(PlacementStep.B1_PROBE, otherLevel.taskId, otherLevel.scenarioId,
            AssessmentResult.FAIL, PlacementTestFixtures.getOutcome(AssessmentResult.FAIL).criteria)
        val checkpoint = initial.copy(records = listOf(record), usedScenarioIds = listOf(otherLevel.scenarioId), currentTaskId = null)
        assertEquals(sharedScenario, PlacementRules.selectTask(PlacementStep.A2_CONFIRMATION, listOf(sharedScenario), checkpoint, random()))
    }

    @Test
    fun probeExcludesAllPreviouslyUsedScenarios() {
        val checkpoint = initial.copy(currentTaskId = null, usedScenarioIds = tasks.map { it.scenarioId })
        assertNull(PlacementRules.selectTask(PlacementStep.B1_PROBE, tasks, checkpoint, random()))
    }

    @Test
    fun insufficientTaskScenarioIsNotMarkedUsed() {
        val retry = apply(initial, insufficient()) as PlacementTransition.RetryTask
        assertEquals(initial.usedScenarioIds, retry.checkpoint.usedScenarioIds)
        assertEquals(initial.records, retry.checkpoint.records)
    }

    @Test
    fun missingAlternativeScenarioStopsWithNoTaskAvailable() {
        val transition = apply(initial, PlacementTestFixtures.getOutcome(AssessmentResult.FAIL), listOf(tasks.first()))
        assertEquals(PlacementTransition.Stopped(PlacementStopReason.NO_TASK_AVAILABLE), transition)
    }

    @Test
    fun selectionIsDeterministicForSeed() {
        assertEquals(PlacementRules.selectTask(PlacementStep.INITIAL_A2, tasks, null, random()),
            PlacementRules.selectTask(PlacementStep.INITIAL_A2, tasks, null, random()))
    }

    @Test
    fun selectionAlwaysMatchesStepLevel() {
        PlacementStep.entries.forEach { step ->
            assertEquals(step.level, checkNotNull(PlacementRules.selectTask(step, tasks, null, random())).targetLevel)
        }
    }

    @Test
    fun insufficientEvidenceRetriesSameTask() {
        val retry = apply(initial, insufficient()) as PlacementTransition.RetryTask
        assertEquals(initial.step, retry.checkpoint.step)
        assertEquals(initial.currentTaskId, retry.checkpoint.currentTaskId)
        assertEquals(1, retry.checkpoint.technicalRetryCount)
        assertEquals(InsufficientEvidenceReason.AUDIO_UNAVAILABLE, retry.reason)
        assertEquals(getTask(initial), PlacementRules.selectTask(initial.step, tasks.reversed(), retry.checkpoint, Random(1)))
    }

    @Test
    fun missingRetryTaskDoesNotChooseReplacement() {
        assertNull(PlacementRules.selectTask(initial.step, tasks.filter { it.taskId != initial.currentTaskId }, initial, random()))
    }

    @Test
    fun insufficientEvidenceDoesNotChangeCandidateLevel() {
        val checkpoint = initial.copy(candidateLevel = SpeakingLevel.B1)
        val retry = apply(checkpoint, insufficient()) as PlacementTransition.RetryTask
        assertEquals(checkpoint.candidateLevel, retry.checkpoint.candidateLevel)
    }

    @Test
    fun thirdInsufficientAttemptStopsWithoutLevel() {
        InsufficientEvidenceReason.entries.filter { it != InsufficientEvidenceReason.NO_USER_SPEECH }.forEach { reason ->
            val first = apply(initial, insufficient(reason)) as PlacementTransition.RetryTask
            val second = apply(first.checkpoint, insufficient(reason)) as PlacementTransition.RetryTask
            assertEquals(2, second.checkpoint.technicalRetryCount)
            assertEquals(PlacementTransition.Stopped(PlacementStopReason.TECHNICAL_RETRY_LIMIT), apply(second.checkpoint, insufficient(reason)))
            assertEquals(SpeakingLevel.NOT_ASSESSED, second.checkpoint.candidateLevel)
        }
    }

    @Test
    fun retryCountResetsAfterValidResult() {
        val next = apply(initial.copy(technicalRetryCount = PlacementRules.MAX_TECHNICAL_RETRIES),
            PlacementTestFixtures.getOutcome(AssessmentResult.PASS)) as PlacementTransition.NextTask
        assertEquals(0, next.checkpoint.technicalRetryCount)
    }

    @Test
    fun noUserSpeechDoesNotConsumeRetry() {
        val checkpoint = initial.copy(technicalRetryCount = PlacementRules.MAX_TECHNICAL_RETRIES)
        val transition = apply(checkpoint, insufficient(InsufficientEvidenceReason.NO_USER_SPEECH)) as PlacementTransition.RetryTask
        assertEquals(checkpoint, transition.checkpoint)
    }

    @Test
    fun candidateTracksHighestPassedProbeAndThenConfirmationLevel() {
        val b1 = apply(initial, PlacementTestFixtures.getOutcome(AssessmentResult.PASS)) as PlacementTransition.NextTask
        assertEquals(SpeakingLevel.A2, b1.checkpoint.candidateLevel)
        val b2 = apply(b1.checkpoint, PlacementTestFixtures.getOutcome(AssessmentResult.PASS)) as PlacementTransition.NextTask
        assertEquals(SpeakingLevel.B1, b2.checkpoint.candidateLevel)
        val confirmation = apply(b2.checkpoint, PlacementTestFixtures.getOutcome(AssessmentResult.PASS)) as PlacementTransition.NextTask
        assertEquals(SpeakingLevel.B2, confirmation.checkpoint.candidateLevel)
        val lower = apply(confirmation.checkpoint, PlacementTestFixtures.getOutcome(AssessmentResult.FAIL)) as PlacementTransition.NextTask
        assertEquals(SpeakingLevel.B1, lower.checkpoint.candidateLevel)
    }

    @Test
    fun eighthValidTaskIsRejected() {
        val record = PlacementTaskRecord(initial.step, getTask(initial).taskId, getTask(initial).scenarioId,
            AssessmentResult.PASS, PlacementTestFixtures.getOutcome(AssessmentResult.PASS).criteria)
        val checkpoint = PlacementTestFixtures.getCheckpoint(PlacementStep.A1_CONFIRMATION)
            .copy(records = List(PlacementRules.MAX_VALID_TASKS) { record })
        assertEquals(PlacementTransition.Stopped(PlacementStopReason.TASK_LIMIT_REACHED),
            apply(checkpoint, PlacementTestFixtures.getOutcome(AssessmentResult.FAIL)))
    }

    @Test
    fun seventhValidTaskCannotStartAnEighth() {
        val record = PlacementTaskRecord(initial.step, getTask(initial).taskId, getTask(initial).scenarioId,
            AssessmentResult.PASS, PlacementTestFixtures.getOutcome(AssessmentResult.PASS).criteria)
        assertEquals(PlacementTransition.Stopped(PlacementStopReason.TASK_LIMIT_REACHED),
            apply(initial.copy(records = List(PlacementRules.MAX_VALID_TASKS - 1) { record }), PlacementTestFixtures.getOutcome(AssessmentResult.PASS)))
    }

    @Test
    fun startBeginsAtInitialA2() {
        assertEquals(PlacementStep.INITIAL_A2, initial.step)
        assertEquals(tasks.first().taskId, initial.currentTaskId)
        assertEquals(PlacementTestFixtures.USER_ID, initial.userId)
        assertEquals(SpeakingLevel.NOT_ASSESSED, initial.candidateLevel)
        assertTrue(initial.records.isEmpty())
        assertTrue(initial.usedScenarioIds.isEmpty())
        assertEquals(0, initial.technicalRetryCount)
        assertNull(initial.pendingResult)
        assertEquals(initial.createdAtMillis + PlacementRules.CHECKPOINT_TTL_MILLIS, initial.expiresAtMillis)
    }

    @Test
    fun checkpointIsValidJustBeforeExpiry() {
        assertFalse(PlacementRules.isExpired(initial, initial.createdAtMillis))
        assertFalse(PlacementRules.isExpired(initial, initial.createdAtMillis + PlacementRules.CHECKPOINT_TTL_MILLIS - 1))
    }

    @Test
    fun checkpointExpiresAtTwentyFourHours() {
        assertTrue(PlacementRules.isExpired(initial, initial.createdAtMillis + PlacementRules.CHECKPOINT_TTL_MILLIS))
    }

    @Test
    fun clockMovedBackwardExpiresCheckpoint() {
        assertTrue(PlacementRules.isExpired(initial, initial.createdAtMillis - 1))
    }

    @Test
    fun clockDifferenceOverflowExpiresCheckpoint() {
        assertTrue(PlacementRules.isExpired(initial.copy(createdAtMillis = Long.MIN_VALUE), Long.MAX_VALUE))
    }

    @Test
    fun resumeKeepsStepRecordsAndUsedScenarios() {
        val next = apply(initial, PlacementTestFixtures.getOutcome(AssessmentResult.PASS)) as PlacementTransition.NextTask
        val snapshot = next.checkpoint.copy()
        assertEquals(getTask(snapshot), PlacementRules.selectTask(snapshot.step, tasks, snapshot, random()))
        assertEquals(snapshot, next.checkpoint)
        assertEquals(1, snapshot.records.size)
        assertEquals(listOf(getTask(initial).scenarioId), snapshot.usedScenarioIds)
    }

    @Test
    fun transitionsAndRetriesNeverExtendExpiry() {
        val next = apply(initial, PlacementTestFixtures.getOutcome(AssessmentResult.PASS)) as PlacementTransition.NextTask
        val retry = apply(next.checkpoint, insufficient()) as PlacementTransition.RetryTask
        assertEquals(initial.createdAtMillis, retry.checkpoint.createdAtMillis)
        assertEquals(initial.expiresAtMillis, retry.checkpoint.expiresAtMillis)
    }

    @Test
    fun restartAfterExpiryBeginsAtInitialA2() {
        val next = apply(initial, PlacementTestFixtures.getOutcome(AssessmentResult.PASS)) as PlacementTransition.NextTask
        val nowMillis = initial.expiresAtMillis
        assertTrue(PlacementRules.isExpired(next.checkpoint, nowMillis))
        val restarted = PlacementRules.start(initial.userId, tasks.first(), nowMillis)
        assertEquals(PlacementStep.INITIAL_A2, restarted.step)
        assertTrue(restarted.records.isEmpty())
        assertTrue(restarted.usedScenarioIds.isEmpty())
        assertEquals(0, restarted.technicalRetryCount)
        assertEquals(nowMillis, restarted.createdAtMillis)
    }

    private fun assertPass(step: PlacementStep, expected: PlacementStep?) {
        assertEquals(expected, PlacementRules.getNextStep(step, AssessmentResult.PASS))
    }

    private fun assertNotPassed(step: PlacementStep, expected: PlacementStep?) {
        assertEquals(expected, PlacementRules.getNextStep(step, AssessmentResult.PARTIAL))
        assertEquals(expected, PlacementRules.getNextStep(step, AssessmentResult.FAIL))
    }

    private fun assertDifferentContext(previous: PlacementStep, next: PlacementStep) {
        val previousTask = tasks.first { it.targetLevel == previous.level }
        val record = PlacementTaskRecord(previous, previousTask.taskId, previousTask.scenarioId,
            AssessmentResult.PASS, PlacementTestFixtures.getOutcome(AssessmentResult.PASS).criteria)
        val checkpoint = initial.copy(step = next, currentTaskId = null, records = listOf(record), usedScenarioIds = listOf(previousTask.scenarioId))
        val selected = checkNotNull(PlacementRules.selectTask(next, tasks, checkpoint, random()))
        assertNotEquals(previousTask.scenarioId, selected.scenarioId)
    }

    private fun assertPath(expected: SpeakingLevel, vararg results: AssessmentResult) {
        var checkpoint = initial
        results.forEachIndexed { index, result ->
            val transition = apply(checkpoint, PlacementTestFixtures.getOutcome(result))
            if (index == results.lastIndex) {
                val confirmed = transition as PlacementTransition.Confirmed
                assertEquals(expected, confirmed.result.level)
                assertEquals(results.size, confirmed.checkpoint.records.size)
                assertEquals(PlacementRole.CONFIRMATION, checkpoint.step.role)
            } else {
                checkpoint = (transition as PlacementTransition.NextTask).checkpoint
            }
        }
    }

    private fun walkPaths(checkpoint: PlacementCheckpoint): List<Int> = AssessmentResult.entries.flatMap { result ->
        when (val transition = apply(checkpoint, PlacementTestFixtures.getOutcome(result))) {
            is PlacementTransition.NextTask -> {
                assertTrue(transition.checkpoint.records.size < PlacementRules.MAX_VALID_TASKS)
                walkPaths(transition.checkpoint)
            }
            is PlacementTransition.Confirmed -> {
                assertEquals(PlacementRole.CONFIRMATION, checkpoint.step.role)
                listOf(transition.checkpoint.records.size)
            }
            else -> error("A complete task bank must confirm every valid path")
        }
    }

    private fun apply(
        checkpoint: PlacementCheckpoint,
        outcome: AssessmentOutcome,
        bank: List<AssessmentTask> = tasks
    ): PlacementTransition = PlacementRules.applyOutcome(checkpoint, getTask(checkpoint), outcome, bank, random(),
        PlacementTestFixtures.CREATED_AT_MILLIS + 1)

    private fun getTask(checkpoint: PlacementCheckpoint): AssessmentTask = tasks.first { it.taskId == checkpoint.currentTaskId }

    private fun insufficient(reason: InsufficientEvidenceReason = InsufficientEvidenceReason.AUDIO_UNAVAILABLE): AssessmentOutcome =
        AssessmentOutcome.InsufficientEvidence(reason)

    private fun random(): Random = Random(PlacementTestFixtures.RANDOM_SEED)
}
