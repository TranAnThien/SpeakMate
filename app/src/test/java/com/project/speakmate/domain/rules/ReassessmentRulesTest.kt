package com.project.speakmate.domain.rules

import com.project.speakmate.domain.model.AssessmentOutcome
import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.AssessmentTask
import com.project.speakmate.domain.model.InsufficientEvidenceReason
import com.project.speakmate.domain.model.ReassessmentState
import com.project.speakmate.domain.model.ReassessmentStep
import com.project.speakmate.domain.model.ReassessmentTransition
import com.project.speakmate.domain.model.ReassessmentTransition.EligibilityReason
import com.project.speakmate.domain.model.ReassessmentTransition.StopReason
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.SpeakingLevel.A1
import com.project.speakmate.domain.model.SpeakingLevel.A2
import com.project.speakmate.domain.model.SpeakingLevel.B1
import com.project.speakmate.domain.model.SpeakingLevel.B2
import com.project.speakmate.domain.model.SpeakingLevel.NOT_ASSESSED
import com.project.speakmate.domain.model.TrainingProgress
import com.project.speakmate.domain.model.TrainingStage
import com.project.speakmate.domain.rules.ReassessmentTestFixtures.ASSESSED_AT_MILLIS
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReassessmentRulesTest {
    private val rules = ReassessmentRules
    private val tasks = ReassessmentTestFixtures.getTasks()
    private val pass = PlacementTestFixtures.getOutcome(AssessmentResult.PASS)
    private val confirmationPass = pass.copy(criteria = pass.criteria.copy(rangeMet = false))
    private val random = Random(ReassessmentTestFixtures.RANDOM_SEED)

    @Test
    fun mapsEveryLevelToItsImmediateSuccessorOrNoSuccessor() {
        assertEquals(A2, A1.getNextLevel())
        assertEquals(B1, A2.getNextLevel())
        assertEquals(B2, B1.getNextLevel())
        assertNull(B2.getNextLevel())
        assertNull(NOT_ASSESSED.getNextLevel())
    }

    @Test
    fun startsCurrentLevelConfirmationForEachEligibleCompletedPath() {
        for (level in listOf(A1, A2, B1)) {
            assertTrue(rules.canStart(ReassessmentTestFixtures.getUser(level)))
            val state = start(level)
            assertEquals(ReassessmentStep.CURRENT_CONFIRMATION, state.step)
            assertEquals(level, state.currentLevel)
            assertEquals(level, state.currentTask?.targetLevel)
            assertTrue(state.usedScenarioIds.isEmpty())
        }
    }

    @Test
    fun rejectsUnassessedAndIncompletePaths() {
        val unassessed = ReassessmentTestFixtures.getUser(NOT_ASSESSED)
        assertFalse(rules.canStart(unassessed))
        assertEquals(
            ReassessmentTransition.NotEligible(EligibilityReason.LEVEL_NOT_ASSESSED),
            rules.start(unassessed, tasks, random)
        )
        val incompleteProgress = listOf(
            null,
            TrainingProgress(0, 0, TrainingStage.UNIT),
            TrainingProgress(0, 0, TrainingStage.GOAL_ASSESSMENT)
        )
        for (progress in incompleteProgress) {
            val user = ReassessmentTestFixtures.getUser(A2, progress)
            assertFalse(rules.canStart(user))
            assertEquals(
                ReassessmentTransition.NotEligible(EligibilityReason.PATH_NOT_COMPLETED),
                rules.start(user, tasks, random)
            )
        }
    }

    @Test
    fun rejectsB2EvenAfterPathCompletion() {
        val user = ReassessmentTestFixtures.getUser(B2)
        assertFalse(rules.canStart(user))
        assertEquals(
            ReassessmentTransition.NotEligible(EligibilityReason.HIGHEST_LEVEL),
            rules.start(user, tasks, random)
        )
    }

    @Test
    fun promotesEveryEligibleLevelOnlyAfterPassingTheNextLevelConfirmation() {
        for ((currentLevel, nextLevel) in listOf(A1 to A2, A2 to B1, B1 to B2)) {
            val initial = start(currentLevel)
            val probe = (apply(initial, pass) as ReassessmentTransition.NextStep).state
            assertEquals(ReassessmentStep.NEXT_PROBE, probe.step)
            assertEquals(nextLevel, probe.currentTask?.targetLevel)
            assertEquals(initial.currentTask?.scenarioId, probe.currentConfirmation?.scenarioId)
            assertEquals(pass.criteria, probe.currentConfirmation?.criteria)

            val confirmation = (apply(probe, pass) as ReassessmentTransition.NextStep).state
            assertEquals(ReassessmentStep.NEXT_CONFIRMATION, confirmation.step)
            assertEquals(nextLevel, confirmation.currentTask?.targetLevel)
            assertNotEquals(probe.currentTask?.scenarioId, confirmation.currentTask?.scenarioId)
            assertEquals(probe.currentTask?.scenarioId, confirmation.nextProbe?.scenarioId)
            assertEquals(pass.criteria, confirmation.nextProbe?.criteria)
            assertEquals(setOf(probe.currentConfirmation?.scenarioId, probe.currentTask?.scenarioId), confirmation.usedScenarioIds)
            assertEquals(
                ReassessmentTransition.Promoted(
                    nextLevel, confirmationPass.criteria, ASSESSED_AT_MILLIS, TrainingProgress(0, 0, TrainingStage.UNIT)
                ),
                apply(confirmation, confirmationPass)
            )
        }
    }

    @Test
    fun passingTheProbeOpensConfirmationWithoutPromoting() {
        val initial = start(A1)
        val probe = (apply(initial, pass) as ReassessmentTransition.NextStep).state
        val transition = apply(probe, pass)
        assertTrue(transition is ReassessmentTransition.NextStep)
        assertEquals(ReassessmentStep.NEXT_CONFIRMATION, (transition as ReassessmentTransition.NextStep).state.step)
    }

    @Test
    fun endsWithoutPromotionOnPartialOrFailAtAnyStep() {
        val initial = start(A2)
        val probe = (apply(initial, pass) as ReassessmentTransition.NextStep).state
        val confirmation = (apply(probe, pass) as ReassessmentTransition.NextStep).state
        for (state in listOf(initial, probe, confirmation)) {
            for (result in listOf(AssessmentResult.PARTIAL, AssessmentResult.FAIL)) {
                assertEquals(
                    ReassessmentTransition.NotPromoted(state.step, result),
                    apply(state, PlacementTestFixtures.getOutcome(result))
                )
            }
        }
    }

    @Test
    fun permitsTwoTechnicalRetriesAndStopsOnTheThirdFailure() {
        val initial = start(A1)
        val reason = InsufficientEvidenceReason.EVALUATION_FAILED
        val failure = AssessmentOutcome.InsufficientEvidence(reason)
        val firstRetry = (apply(initial, failure) as ReassessmentTransition.RetryTask).state
        assertEquals(initial.copy(technicalRetryCount = 1), firstRetry)
        val secondRetry = (apply(firstRetry, failure) as ReassessmentTransition.RetryTask).state
        assertEquals(initial.copy(technicalRetryCount = PlacementRules.MAX_TECHNICAL_RETRIES), secondRetry)
        assertEquals(ReassessmentTransition.Stopped(StopReason.TECHNICAL_RETRY_LIMIT), apply(secondRetry, failure))
    }

    @Test
    fun retriesNoSpeechOnTheSameTaskWithoutCountingEvenAtTheTechnicalLimit() {
        val initial = start(A1)
        val reason = InsufficientEvidenceReason.NO_USER_SPEECH
        for (state in listOf(initial, initial.copy(technicalRetryCount = PlacementRules.MAX_TECHNICAL_RETRIES))) {
            assertEquals(ReassessmentTransition.RetryTask(state, reason), apply(state, AssessmentOutcome.InsufficientEvidence(reason)))
            assertEquals(state.currentTask, rules.selectTask(state, tasks, random))
        }
    }

    @Test
    fun resetsTheTechnicalCounterWhenMovingToAnotherStep() {
        val initial = start(A1).copy(technicalRetryCount = PlacementRules.MAX_TECHNICAL_RETRIES)
        val probe = (apply(initial, pass) as ReassessmentTransition.NextStep).state
        assertEquals(0, probe.technicalRetryCount)
    }

    @Test
    fun stopsWhenAnyStepHasNoEligibleTask() {
        val user = ReassessmentTestFixtures.getUser(A1)
        val initial = start(A1)
        val probe = (apply(initial, pass) as ReassessmentTransition.NextStep).state
        val unavailable = ReassessmentTransition.Stopped(StopReason.NO_TASK_AVAILABLE)
        assertEquals(unavailable, rules.start(user, emptyList(), random))
        assertEquals(unavailable, apply(initial, pass, tasks.filter { it.targetLevel != A2 }))
        val sameContext = requireNotNull(probe.currentTask).copy(taskId = "same-context")
        assertEquals(unavailable, apply(probe, pass, listOf(sameContext, ReassessmentTestFixtures.getTask(B1))))
    }

    @Test
    fun rejectsAnOutcomeFromTheOldProbeOrAReplacementConfirmationInItsContext() {
        val initial = start(A1)
        val probe = (apply(initial, pass) as ReassessmentTransition.NextStep).state
        val confirmation = (apply(probe, pass) as ReassessmentTransition.NextStep).state
        val oldTask = requireNotNull(probe.currentTask)
        val repeatedContextTask = requireNotNull(confirmation.currentTask).copy(scenarioId = oldTask.scenarioId)
        val unexpected = ReassessmentTransition.Stopped(StopReason.UNEXPECTED_TASK)
        assertEquals(unexpected, rules.applyOutcome(confirmation, oldTask, pass, tasks, random, ASSESSED_AT_MILLIS))
        assertEquals(unexpected, apply(confirmation.copy(currentTask = repeatedContextTask), pass))
    }

    @Test
    fun aB2StateWithFabricatedConfirmationCannotPromote() {
        val initial = start(B1)
        val probe = (apply(initial, pass) as ReassessmentTransition.NextStep).state
        val confirmation = (apply(probe, pass) as ReassessmentTransition.NextStep).state
        val invalid = confirmation.copy(currentLevel = B2)
        assertEquals(ReassessmentTransition.Stopped(StopReason.INVALID_STATE), apply(invalid, pass))
        assertNull(rules.selectTask(invalid, tasks, random))
    }

    @Test
    fun rejectsAProbeRecordWithoutACurrentLevelConfirmation() {
        val initial = start(A1)
        val probe = (apply(initial, pass) as ReassessmentTransition.NextStep).state
        val confirmation = (apply(probe, pass) as ReassessmentTransition.NextStep).state
        val invalid = confirmation.copy(currentConfirmation = null)
        assertEquals(ReassessmentTransition.Stopped(StopReason.INVALID_STATE), apply(invalid, pass))
    }

    private fun start(level: SpeakingLevel): ReassessmentState =
        (rules.start(ReassessmentTestFixtures.getUser(level), tasks, random) as ReassessmentTransition.NextStep).state

    private fun apply(
        state: ReassessmentState,
        outcome: AssessmentOutcome,
        availableTasks: List<AssessmentTask> = tasks
    ): ReassessmentTransition =
        rules.applyOutcome(state, requireNotNull(state.currentTask), outcome, availableTasks, random, ASSESSED_AT_MILLIS)
}
