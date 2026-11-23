package com.project.speakmate.domain.rules

import com.project.speakmate.domain.model.AssessmentEvidence
import com.project.speakmate.domain.model.AssessmentOutcome
import com.project.speakmate.domain.model.ContentStatus
import com.project.speakmate.domain.model.ContentStatus.COMPLETED
import com.project.speakmate.domain.model.ContentStatus.CURRENT
import com.project.speakmate.domain.model.ContentStatus.LOCKED
import com.project.speakmate.domain.model.ProgressHoldReason
import com.project.speakmate.domain.model.ProgressProblem
import com.project.speakmate.domain.model.SpeakingEvaluation
import com.project.speakmate.domain.model.TrainingActivity.GoalAssessmentActivity
import com.project.speakmate.domain.model.TrainingActivity.UnitActivity
import com.project.speakmate.domain.model.TrainingProgress
import com.project.speakmate.domain.model.TrainingProgressDecision
import com.project.speakmate.domain.model.TrainingProgressDecision.AdvanceReason
import com.project.speakmate.domain.model.TrainingStage
import com.project.speakmate.domain.model.TrainingStage.GOAL_ASSESSMENT
import com.project.speakmate.domain.model.TrainingStage.PATH_COMPLETED
import com.project.speakmate.domain.model.TrainingStage.UNIT
import com.project.speakmate.domain.model.UnitPreparation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingProgressRulesTest {
    private val path = TrainingProgressTestFixtures.getPath()
    private val rules = TrainingProgressRules
    private val pass = getOutcome(AssessmentTestFixtures.getPassEvidenceSet())
    private val partial = getOutcome(AssessmentTestFixtures.getPartialEvidenceSet())
    private val fail = getOutcome(AssessmentTestFixtures.getFailEvidenceSet())
    private val insufficientEvidence = getOutcome(AssessmentTestFixtures.getInsufficientEvidenceSet())

    @Test
    fun progressOutcomeUsesStructureWithoutPreparationContent() {
        val structure = path.copy(goals = path.goals.map { goal ->
            goal.copy(units = goal.units.map { unit ->
                unit.copy(preparation = UnitPreparation(emptyList(), emptyList(), emptyList()))
            })
        })
        val progress = TrainingProgress(0, 0, UNIT)
        assertEquals(rules.applyOutcome(path, progress, UnitActivity(0, 0), pass),
            rules.applyOutcome(structure, progress, UnitActivity(0, 0), pass))
    }

    @Test
    fun countsUnitsAndOneAssessmentForEveryGoal() {
        assertEquals(9, rules.getTotalActivityCount(path))
        assertEquals(0, rules.getCompletedActivityCount(path, TrainingProgress(0, 0, UNIT)))
        assertEquals(1, rules.getCompletedActivityCount(path, TrainingProgress(0, 1, UNIT)))
        assertEquals(3, rules.getCompletedActivityCount(path, TrainingProgress(0, 2, GOAL_ASSESSMENT)))
        assertEquals(4, rules.getCompletedActivityCount(path, TrainingProgress(1, 0, UNIT)))
        assertEquals(9, rules.getCompletedActivityCount(path, TrainingProgress(2, 0, PATH_COMPLETED)))
    }

    @Test
    fun replayKeepsTheCompletedActivityCountUnchanged() {
        val progress = TrainingProgress(1, 0, UNIT)
        val decision = rules.applyOutcome(path, progress, UnitActivity(0, 0), pass)
        assertEquals(TrainingProgressDecision.Unchanged(ProgressHoldReason.REPLAY), decision)
        assertEquals(4, rules.getCompletedActivityCount(path, progress))
    }

    @Test
    fun emptyAndInvalidProgressDoNotBecomeCompletedActivities() {
        val empty = path.copy(goals = emptyList())
        assertEquals(0, rules.getTotalActivityCount(empty))
        assertEquals(0, rules.getCompletedActivityCount(empty, TrainingProgress(0, 0, UNIT)))
        assertEquals(0, rules.getCompletedActivityCount(path, TrainingProgress(-1, 0, UNIT)))
        val assessmentOnly = TrainingProgressTestFixtures.getPath(listOf(0))
        assertEquals(1, rules.getTotalActivityCount(assessmentOnly))
        assertEquals(0, rules.getCompletedActivityCount(assessmentOnly, TrainingProgress(0, 0, GOAL_ASSESSMENT)))
        assertEquals(1, rules.getCompletedActivityCount(assessmentOnly, TrainingProgress(0, 0, PATH_COMPLETED)))
    }

    @Test
    fun marksOnlyTheActiveUnitCurrentAtEachPosition() {
        assertStatuses(
            TrainingProgress(0, 0, UNIT),
            listOf(CURRENT, LOCKED, LOCKED),
            listOf(listOf(CURRENT, LOCKED, LOCKED), listOf(LOCKED, LOCKED), listOf(LOCKED)),
            listOf(LOCKED, LOCKED, LOCKED)
        )
        assertStatuses(
            TrainingProgress(0, 1, UNIT),
            listOf(CURRENT, LOCKED, LOCKED),
            listOf(listOf(COMPLETED, CURRENT, LOCKED), listOf(LOCKED, LOCKED), listOf(LOCKED)),
            listOf(LOCKED, LOCKED, LOCKED)
        )
        assertStatuses(
            TrainingProgress(0, 2, UNIT),
            listOf(CURRENT, LOCKED, LOCKED),
            listOf(listOf(COMPLETED, COMPLETED, CURRENT), listOf(LOCKED, LOCKED), listOf(LOCKED)),
            listOf(LOCKED, LOCKED, LOCKED)
        )
        assertStatuses(
            TrainingProgress(1, 0, UNIT),
            listOf(COMPLETED, CURRENT, LOCKED),
            listOf(listOf(COMPLETED, COMPLETED, COMPLETED), listOf(CURRENT, LOCKED), listOf(LOCKED)),
            listOf(COMPLETED, LOCKED, LOCKED)
        )
    }

    @Test
    fun marksAllUnitsInTheActiveGoalCompletedDuringItsAssessment() {
        assertStatuses(
            TrainingProgress(1, 1, GOAL_ASSESSMENT),
            listOf(COMPLETED, CURRENT, LOCKED),
            listOf(listOf(COMPLETED, COMPLETED, COMPLETED), listOf(COMPLETED, COMPLETED), listOf(LOCKED)),
            listOf(COMPLETED, CURRENT, LOCKED)
        )
    }

    @Test
    fun marksEveryActivityCompletedAfterThePathEnds() {
        assertStatuses(
            TrainingProgress(2, 0, PATH_COMPLETED),
            listOf(COMPLETED, COMPLETED, COMPLETED),
            listOf(listOf(COMPLETED, COMPLETED, COMPLETED), listOf(COMPLETED, COMPLETED), listOf(COMPLETED)),
            listOf(COMPLETED, COMPLETED, COMPLETED)
        )
    }

    @Test
    fun completedPathWithEmptyGoalsKeepsAssessmentsReplayableAndRejectsPhantomUnits() {
        val completedPath = TrainingProgressTestFixtures.getPath(listOf(0, 0))
        val progress = TrainingProgress(1, 0, PATH_COMPLETED)
        completedPath.goals.indices.forEach { goalIndex ->
            val assessment = GoalAssessmentActivity(goalIndex)
            assertEquals(COMPLETED, rules.getGoalStatus(completedPath, progress, goalIndex))
            assertEquals(COMPLETED, rules.getGoalAssessmentStatus(completedPath, progress, goalIndex))
            assertTrue(rules.canOpen(completedPath, progress, assessment))
            assertTrue(rules.isReplay(completedPath, progress, assessment))
            assertEquals(TrainingProgressDecision.Unchanged(ProgressHoldReason.REPLAY), rules.applyOutcome(completedPath, progress, assessment, pass))
            assertEquals(LOCKED, rules.getUnitStatus(completedPath, progress, goalIndex, 0))
            assertFalse(rules.canOpen(completedPath, progress, UnitActivity(goalIndex, 0)))
        }
    }

    @Test
    fun passingTheCurrentMiddleUnitOpensTheNextUnit() {
        assertEquals(
            TrainingProgressDecision.Advanced(TrainingProgress(0, 2, UNIT), AdvanceReason.NEXT_UNIT_OPENED),
            rules.applyOutcome(path, TrainingProgress(0, 1, UNIT), UnitActivity(0, 1), pass)
        )
    }

    @Test
    fun passingTheLastUnitOpensTheAssessmentAndKeepsTheUnitIndex() {
        assertEquals(
            TrainingProgressDecision.Advanced(
                TrainingProgress(0, 2, GOAL_ASSESSMENT),
                AdvanceReason.GOAL_ASSESSMENT_OPENED
            ),
            rules.applyOutcome(path, TrainingProgress(0, 2, UNIT), UnitActivity(0, 2), pass)
        )
    }

    @Test
    fun passingTheMiddleGoalAssessmentOpensTheFirstUnitOfTheNextGoal() {
        assertEquals(
            TrainingProgressDecision.Advanced(TrainingProgress(2, 0, UNIT), AdvanceReason.NEXT_GOAL_OPENED),
            rules.applyOutcome(path, TrainingProgress(1, 1, GOAL_ASSESSMENT), GoalAssessmentActivity(1), pass)
        )
    }

    @Test
    fun passingTheFinalGoalAssessmentCompletesThePathAndKeepsBothIndices() {
        val finalPath = TrainingProgressTestFixtures.getPath(listOf(1, 2))
        assertEquals(
            TrainingProgressDecision.PathCompleted(TrainingProgress(1, 1, PATH_COMPLETED)),
            rules.applyOutcome(finalPath, TrainingProgress(1, 1, GOAL_ASSESSMENT), GoalAssessmentActivity(1), pass)
        )
    }

    @Test
    fun partialAndFailLeaveTheCurrentUnitAndAssessmentAvailableForRetry() {
        val unitProgress = TrainingProgress(1, 1, UNIT)
        val assessmentProgress = unitProgress.copy(stage = GOAL_ASSESSMENT)
        val currentUnit = UnitActivity(1, 1)
        val currentAssessment = GoalAssessmentActivity(1)
        val unchanged = TrainingProgressDecision.Unchanged(ProgressHoldReason.NOT_PASSED)

        for (outcome in listOf(partial, fail)) {
            assertEquals(unchanged, rules.applyOutcome(path, unitProgress, currentUnit, outcome))
            assertEquals(unchanged, rules.applyOutcome(path, assessmentProgress, currentAssessment, outcome))
        }
        assertTrue(rules.canOpen(path, unitProgress, currentUnit))
        assertTrue(rules.canOpen(path, assessmentProgress, currentAssessment))
        assertFalse(rules.isReplay(path, unitProgress, currentUnit))
        assertFalse(rules.isReplay(path, assessmentProgress, currentAssessment))
    }

    @Test
    fun insufficientEvidenceLeavesCurrentProgressAvailableForRetry() {
        val unchanged = TrainingProgressDecision.Unchanged(ProgressHoldReason.INSUFFICIENT_EVIDENCE)
        assertEquals(
            unchanged,
            rules.applyOutcome(path, TrainingProgress(0, 0, UNIT), UnitActivity(0, 0), insufficientEvidence)
        )
        assertEquals(
            unchanged,
            rules.applyOutcome(
                path, TrainingProgress(0, 2, GOAL_ASSESSMENT), GoalAssessmentActivity(0), insufficientEvidence
            )
        )
    }

    @Test
    fun replayingAnEarlierUnitOrAssessmentNeverMovesProgress() {
        val progress = TrainingProgress(1, 1, UNIT)
        val replayActivities = listOf(UnitActivity(1, 0), UnitActivity(0, 2), GoalAssessmentActivity(0))
        val unchanged = TrainingProgressDecision.Unchanged(ProgressHoldReason.REPLAY)

        for (activity in replayActivities) {
            assertTrue(rules.canOpen(path, progress, activity))
            assertTrue(rules.isReplay(path, progress, activity))
            for (outcome in listOf(pass, partial, fail, insufficientEvidence)) {
                assertEquals(unchanged, rules.applyOutcome(path, progress, activity, outcome))
            }
        }
        assertEquals(
            unchanged,
            rules.applyOutcome(path, progress.copy(stage = GOAL_ASSESSMENT), UnitActivity(1, 1), pass)
        )
    }

    @Test
    fun rejectsOutcomesForLockedActivities() {
        val progress = TrainingProgress(1, 0, UNIT)
        val invalid = TrainingProgressDecision.Invalid(ProgressProblem.ACTIVITY_LOCKED)
        for (activity in listOf(UnitActivity(1, 1), GoalAssessmentActivity(1), UnitActivity(2, 0))) {
            assertFalse(rules.canOpen(path, progress, activity))
            assertFalse(rules.isReplay(path, progress, activity))
            assertEquals(invalid, rules.applyOutcome(path, progress, activity, pass))
        }
    }

    @Test
    fun everyOutcomeAfterPathCompletionIsAReplay() {
        val progress = TrainingProgress(2, 0, PATH_COMPLETED)
        val unchanged = TrainingProgressDecision.Unchanged(ProgressHoldReason.REPLAY)
        for (activity in listOf(UnitActivity(2, 0), GoalAssessmentActivity(2))) {
            assertTrue(rules.canOpen(path, progress, activity))
            assertTrue(rules.isReplay(path, progress, activity))
            for (outcome in listOf(pass, partial, fail, insufficientEvidence)) {
                assertEquals(unchanged, rules.applyOutcome(path, progress, activity, outcome))
            }
        }
    }

    @Test
    fun rejectsNegativeAndOutOfRangeProgressForEveryStage() {
        for (stage in TrainingStage.entries) {
            val invalidProgress = mapOf(
                TrainingProgress(-1, 0, stage) to ProgressProblem.GOAL_OUT_OF_RANGE,
                TrainingProgress(path.goals.size, 0, stage) to ProgressProblem.GOAL_OUT_OF_RANGE,
                TrainingProgress(0, -1, stage) to ProgressProblem.UNIT_OUT_OF_RANGE,
                TrainingProgress(0, path.goals[0].units.size, stage) to ProgressProblem.UNIT_OUT_OF_RANGE
            )
            val unit = UnitActivity(0, 0)
            for ((progress, problem) in invalidProgress) {
                assertEquals(problem, rules.validate(path, progress))
                assertEquals(LOCKED, rules.getGoalStatus(path, progress, 0))
                assertEquals(LOCKED, rules.getUnitStatus(path, progress, 0, 0))
                assertEquals(LOCKED, rules.getGoalAssessmentStatus(path, progress, 0))
                assertFalse(rules.canOpen(path, progress, unit))
                assertEquals(TrainingProgressDecision.Invalid(problem), rules.applyOutcome(path, progress, unit, pass))
            }
        }
    }

    @Test
    fun rejectsUnknownActivityIndicesEvenAfterPathCompletion() {
        val progress = TrainingProgress(2, 0, PATH_COMPLETED)
        val invalid = TrainingProgressDecision.Invalid(ProgressProblem.ACTIVITY_OUT_OF_RANGE)
        val activities = listOf(
            UnitActivity(2, -1), UnitActivity(2, 1), GoalAssessmentActivity(-1), GoalAssessmentActivity(path.goals.size)
        )
        for (activity in activities) {
            assertFalse(rules.canOpen(path, progress, activity))
            assertEquals(invalid, rules.applyOutcome(path, progress, activity, pass))
        }
    }

    @Test
    fun returnsAConfigurationProblemForAPathWithoutGoals() {
        val emptyPath = TrainingProgressTestFixtures.getPath(emptyList())
        val progress = TrainingProgress(0, 0, UNIT)
        assertEquals(ProgressProblem.EMPTY_PATH, rules.validate(emptyPath, progress))
        assertEquals(
            TrainingProgressDecision.Invalid(ProgressProblem.EMPTY_PATH),
            rules.applyOutcome(emptyPath, progress, GoalAssessmentActivity(0), pass)
        )
    }

    @Test
    fun opensTheAssessmentImmediatelyWhenTheFirstGoalHasNoUnits() {
        val emptyFirstGoalPath = TrainingProgressTestFixtures.getPath(listOf(0, 1))
        val progress = TrainingProgress(0, 0, UNIT)
        assertNull(rules.validate(emptyFirstGoalPath, progress))
        assertEquals(CURRENT, rules.getGoalStatus(emptyFirstGoalPath, progress, 0))
        assertEquals(CURRENT, rules.getGoalAssessmentStatus(emptyFirstGoalPath, progress, 0))
        assertFalse(rules.canOpen(emptyFirstGoalPath, progress, UnitActivity(0, 0)))
        assertEquals(
            TrainingProgressDecision.Advanced(TrainingProgress(1, 0, UNIT), AdvanceReason.NEXT_GOAL_OPENED),
            rules.applyOutcome(emptyFirstGoalPath, progress, GoalAssessmentActivity(0), pass)
        )
        assertEquals(
            ProgressProblem.UNIT_OUT_OF_RANGE,
            rules.validate(emptyFirstGoalPath, progress.copy(currentUnitOrder = 1))
        )
    }

    @Test
    fun advancingToAnEmptyGoalOpensItsAssessmentAndFinishesThePathOnPass() {
        val emptyLastGoalPath = TrainingProgressTestFixtures.getPath(listOf(1, 0))
        val initialProgress = TrainingProgress(0, 0, GOAL_ASSESSMENT)
        val nextProgress = TrainingProgress(1, 0, GOAL_ASSESSMENT)
        assertEquals(
            TrainingProgressDecision.Advanced(nextProgress, AdvanceReason.NEXT_GOAL_OPENED),
            rules.applyOutcome(emptyLastGoalPath, initialProgress, GoalAssessmentActivity(0), pass)
        )
        assertEquals(
            TrainingProgressDecision.PathCompleted(nextProgress.copy(stage = PATH_COMPLETED)),
            rules.applyOutcome(emptyLastGoalPath, nextProgress, GoalAssessmentActivity(1), pass)
        )
        assertNull(rules.validate(emptyLastGoalPath, nextProgress.copy(stage = PATH_COMPLETED)))
    }

    private fun getOutcome(
        evidenceSet: Pair<AssessmentEvidence, SpeakingEvaluation>
    ): AssessmentOutcome = AssessmentRules.assess(evidenceSet.first, evidenceSet.second)

    private fun assertStatuses(
        progress: TrainingProgress,
        goalStatuses: List<ContentStatus>,
        unitStatuses: List<List<ContentStatus>>,
        assessmentStatuses: List<ContentStatus>
    ) {
        assertEquals(goalStatuses, path.goals.indices.map { rules.getGoalStatus(path, progress, it) })
        assertEquals(goalStatuses, path.goals.indices.map { rules.getGoalStatus(path, progress, it) })
        assertEquals(assessmentStatuses, path.goals.indices.map { rules.getGoalAssessmentStatus(path, progress, it) })
        assertEquals(
            unitStatuses,
            path.goals.mapIndexed { goalIndex, goal ->
                goal.units.indices.map { rules.getUnitStatus(path, progress, goalIndex, it) }
            }
        )
        assertEquals(
            unitStatuses,
            path.goals.mapIndexed { goalIndex, goal ->
                goal.units.indices.map { rules.getUnitStatus(goal, progress, goalIndex, it) }
            }
        )
    }
}
