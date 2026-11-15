package com.project.speakmate.domain.rules

import com.project.speakmate.domain.model.AssessmentOutcome
import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.ContentStatus
import com.project.speakmate.domain.model.ProgressHoldReason
import com.project.speakmate.domain.model.ProgressProblem
import com.project.speakmate.domain.model.TrainingActivity
import com.project.speakmate.domain.model.TrainingGoal
import com.project.speakmate.domain.model.TrainingPath
import com.project.speakmate.domain.model.TrainingProgress
import com.project.speakmate.domain.model.TrainingProgressDecision
import com.project.speakmate.domain.model.TrainingProgressDecision.AdvanceReason
import com.project.speakmate.domain.model.TrainingStage

object TrainingProgressRules {
    fun getGoalStatus(path: TrainingPath, progress: TrainingProgress, goalIndex: Int): ContentStatus {
        if (goalIndex !in path.goals.indices || validate(path, progress) != null) {
            return ContentStatus.LOCKED
        }
        return getGoalStatus(progress, goalIndex)
    }

    fun getUnitStatus(
        path: TrainingPath,
        progress: TrainingProgress,
        goalIndex: Int,
        unitIndex: Int
    ): ContentStatus {
        val goal = path.goals.getOrNull(goalIndex) ?: return ContentStatus.LOCKED
        if (validate(path, progress) != null) return ContentStatus.LOCKED
        return getUnitStatus(goal, progress, goalIndex, unitIndex)
    }

    fun getUnitStatus(
        goal: TrainingGoal,
        progress: TrainingProgress,
        goalIndex: Int,
        unitIndex: Int
    ): ContentStatus {
        if (goalIndex < 0 || unitIndex !in goal.units.indices ||
            progress.currentGoalOrder < 0 || progress.currentUnitOrder < 0
        ) {
            return ContentStatus.LOCKED
        }
        return when (getGoalStatus(progress, goalIndex)) {
            ContentStatus.COMPLETED -> ContentStatus.COMPLETED
            ContentStatus.LOCKED -> ContentStatus.LOCKED
            ContentStatus.CURRENT -> when {
                progress.currentUnitOrder !in goal.units.indices -> ContentStatus.LOCKED
                progress.stage == TrainingStage.GOAL_ASSESSMENT -> ContentStatus.COMPLETED
                unitIndex < progress.currentUnitOrder -> ContentStatus.COMPLETED
                unitIndex == progress.currentUnitOrder -> ContentStatus.CURRENT
                else -> ContentStatus.LOCKED
            }
        }
    }

    fun getGoalAssessmentStatus(
        path: TrainingPath,
        progress: TrainingProgress,
        goalIndex: Int
    ): ContentStatus {
        val goal = path.goals.getOrNull(goalIndex) ?: return ContentStatus.LOCKED
        return when (val status = getGoalStatus(path, progress, goalIndex)) {
            ContentStatus.CURRENT -> if (progress.stage == TrainingStage.GOAL_ASSESSMENT || goal.units.isEmpty()) {
                ContentStatus.CURRENT
            } else {
                ContentStatus.LOCKED
            }
            else -> status
        }
    }

    fun validate(path: TrainingPath, progress: TrainingProgress): ProgressProblem? {
        if (path.goals.isEmpty()) return ProgressProblem.EMPTY_PATH
        val goal = path.goals.getOrNull(progress.currentGoalOrder) ?: return ProgressProblem.GOAL_OUT_OF_RANGE
        val units = goal.units
        val unitIndex = progress.currentUnitOrder
        if (units.isEmpty() && unitIndex == 0) return null
        if (unitIndex !in units.indices) return ProgressProblem.UNIT_OUT_OF_RANGE
        return null
    }

    fun isReplay(path: TrainingPath, progress: TrainingProgress, activity: TrainingActivity): Boolean =
        getActivityStatus(path, progress, activity) == ContentStatus.COMPLETED

    fun canOpen(path: TrainingPath, progress: TrainingProgress, activity: TrainingActivity): Boolean =
        getActivityStatus(path, progress, activity) != ContentStatus.LOCKED

    fun applyOutcome(
        path: TrainingPath,
        progress: TrainingProgress,
        activity: TrainingActivity,
        outcome: AssessmentOutcome
    ): TrainingProgressDecision {
        val problem = validate(path, progress)
        if (problem != null) return TrainingProgressDecision.Invalid(problem)
        val goal = path.goals.getOrNull(activity.goalIndex)
            ?: return TrainingProgressDecision.Invalid(ProgressProblem.ACTIVITY_OUT_OF_RANGE)
        if (activity is TrainingActivity.UnitActivity && activity.unitIndex !in goal.units.indices) {
            return TrainingProgressDecision.Invalid(ProgressProblem.ACTIVITY_OUT_OF_RANGE)
        }
        when (getActivityStatus(path, progress, activity)) {
            ContentStatus.LOCKED -> return TrainingProgressDecision.Invalid(ProgressProblem.ACTIVITY_LOCKED)
            ContentStatus.COMPLETED -> return TrainingProgressDecision.Unchanged(ProgressHoldReason.REPLAY)
            ContentStatus.CURRENT -> Unit
        }
        when (outcome) {
            is AssessmentOutcome.InsufficientEvidence ->
                return TrainingProgressDecision.Unchanged(ProgressHoldReason.INSUFFICIENT_EVIDENCE)
            is AssessmentOutcome.Evaluated ->
                if (outcome.result != AssessmentResult.PASS) {
                    return TrainingProgressDecision.Unchanged(ProgressHoldReason.NOT_PASSED)
                }
        }
        return when (activity) {
            is TrainingActivity.UnitActivity -> advanceUnit(goal, progress)
            is TrainingActivity.GoalAssessmentActivity -> advanceGoal(path, progress)
        }
    }

    private fun getGoalStatus(progress: TrainingProgress, goalIndex: Int): ContentStatus = when {
        progress.stage == TrainingStage.PATH_COMPLETED -> ContentStatus.COMPLETED
        goalIndex < progress.currentGoalOrder -> ContentStatus.COMPLETED
        goalIndex == progress.currentGoalOrder -> ContentStatus.CURRENT
        else -> ContentStatus.LOCKED
    }

    private fun getActivityStatus(
        path: TrainingPath,
        progress: TrainingProgress,
        activity: TrainingActivity
    ): ContentStatus = when (activity) {
        is TrainingActivity.UnitActivity -> getUnitStatus(path, progress, activity.goalIndex, activity.unitIndex)
        is TrainingActivity.GoalAssessmentActivity -> getGoalAssessmentStatus(path, progress, activity.goalIndex)
    }

    private fun advanceUnit(goal: TrainingGoal, progress: TrainingProgress): TrainingProgressDecision.Advanced {
        if (progress.currentUnitOrder == goal.units.lastIndex) {
            return TrainingProgressDecision.Advanced(
                progress.copy(stage = TrainingStage.GOAL_ASSESSMENT),
                AdvanceReason.GOAL_ASSESSMENT_OPENED
            )
        }
        return TrainingProgressDecision.Advanced(
            progress.copy(currentUnitOrder = progress.currentUnitOrder + 1),
            AdvanceReason.NEXT_UNIT_OPENED
        )
    }

    private fun advanceGoal(path: TrainingPath, progress: TrainingProgress): TrainingProgressDecision {
        val nextGoalIndex = progress.currentGoalOrder + 1
        val nextGoal = path.goals.getOrNull(nextGoalIndex)
            ?: return TrainingProgressDecision.PathCompleted(progress.copy(stage = TrainingStage.PATH_COMPLETED))
        val nextStage = if (nextGoal.units.isEmpty()) TrainingStage.GOAL_ASSESSMENT else TrainingStage.UNIT
        return TrainingProgressDecision.Advanced(
            TrainingProgress(nextGoalIndex, 0, nextStage),
            AdvanceReason.NEXT_GOAL_OPENED
        )
    }
}

