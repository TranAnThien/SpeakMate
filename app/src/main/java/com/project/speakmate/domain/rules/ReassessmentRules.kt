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
import com.project.speakmate.domain.model.User
import kotlin.random.Random

object ReassessmentRules {
    fun getEvidenceExpiryMillis(startedAtMillis: Long): Long =
        startedAtMillis + PracticeCompletionRules.AUDIO_RETENTION_MILLIS

    fun canSaveResult(
        profile: User,
        result: ReassessmentTransition.Promoted,
        currentLevel: SpeakingLevel
    ): Boolean {
        val isAlreadySaved =
            profile.level == result.newLevel &&
                profile.lastAssessedAt?.time == result.lastAssessedAtMillis &&
                profile.latestAssessmentCriteria == result.latestAssessmentCriteria
        return isAlreadySaved || (profile.level == currentLevel && canStart(profile))
    }

    fun canStart(user: User): Boolean = user.canStartReassessment()

    fun start(user: User, tasks: List<AssessmentTask>, random: Random): ReassessmentTransition {
        if (!canStart(user)) {
            val reason = when (user.level) {
                SpeakingLevel.NOT_ASSESSED -> EligibilityReason.LEVEL_NOT_ASSESSED
                SpeakingLevel.B2 -> EligibilityReason.HIGHEST_LEVEL
                else -> EligibilityReason.PATH_NOT_COMPLETED
            }
            return ReassessmentTransition.NotEligible(reason)
        }
        val nextLevel = user.level.getNextLevel()
            ?: return ReassessmentTransition.Stopped(StopReason.INVALID_STATE)
        return openStep(ReassessmentState(user.level, nextLevel), tasks, random)
    }

    fun selectTask(state: ReassessmentState, tasks: List<AssessmentTask>, random: Random): AssessmentTask? {
        if (!isValid(state)) return null
        val currentTask = state.currentTask
        return tasks.filter { isTaskForStep(state, it) && (currentTask == null || it == currentTask) }
            .randomOrNull(random)
    }

    fun applyOutcome(
        state: ReassessmentState,
        task: AssessmentTask,
        outcome: AssessmentOutcome,
        tasks: List<AssessmentTask>,
        random: Random,
        nowMillis: Long
    ): ReassessmentTransition {
        if (!isValid(state)) return ReassessmentTransition.Stopped(StopReason.INVALID_STATE)
        if (task != state.currentTask || !isTaskForStep(state, task)) {
            return ReassessmentTransition.Stopped(StopReason.UNEXPECTED_TASK)
        }
        return when (outcome) {
            is AssessmentOutcome.InsufficientEvidence -> retry(state, outcome.reason)
            is AssessmentOutcome.Evaluated -> applyEvaluated(state, task, outcome, tasks, random, nowMillis)
        }
    }

    private fun openStep(
        state: ReassessmentState,
        tasks: List<AssessmentTask>,
        random: Random
    ): ReassessmentTransition {
        val task = selectTask(state, tasks, random)
            ?: return ReassessmentTransition.Stopped(StopReason.NO_TASK_AVAILABLE)
        return ReassessmentTransition.NextStep(state.copy(currentTask = task))
    }

    private fun isValid(state: ReassessmentState): Boolean =
        state.currentLevel.getNextLevel() == state.nextLevel &&
            (state.nextProbe == null || state.currentConfirmation != null)

    private fun isTaskForStep(state: ReassessmentState, task: AssessmentTask): Boolean = when (state.step) {
        ReassessmentStep.CURRENT_CONFIRMATION -> task.targetLevel == state.currentLevel
        ReassessmentStep.NEXT_PROBE -> task.targetLevel == state.nextLevel
        ReassessmentStep.NEXT_CONFIRMATION ->
            task.targetLevel == state.nextLevel && task.scenarioId != state.nextProbe?.scenarioId
    }

    private fun retry(state: ReassessmentState, reason: InsufficientEvidenceReason): ReassessmentTransition {
        if (state.technicalRetryCount >= PlacementRules.MAX_TECHNICAL_RETRIES) {
            return ReassessmentTransition.Stopped(StopReason.TECHNICAL_RETRY_LIMIT)
        }
        return ReassessmentTransition.RetryTask(
            state.copy(technicalRetryCount = state.technicalRetryCount + 1),
            reason
        )
    }

    private fun applyEvaluated(
        state: ReassessmentState,
        task: AssessmentTask,
        outcome: AssessmentOutcome.Evaluated,
        tasks: List<AssessmentTask>,
        random: Random,
        nowMillis: Long
    ): ReassessmentTransition {
        if (outcome.result != AssessmentResult.PASS) {
            return ReassessmentTransition.NotPromoted(state.step, outcome.result)
        }
        val updatedState = state.copy(currentTask = null, technicalRetryCount = 0)
        val record = ReassessmentState.PassedTask(task.scenarioId, outcome.criteria)
        return when (state.step) {
            ReassessmentStep.CURRENT_CONFIRMATION ->
                openStep(updatedState.copy(currentConfirmation = record), tasks, random)
            ReassessmentStep.NEXT_PROBE ->
                openStep(updatedState.copy(nextProbe = record), tasks, random)
            ReassessmentStep.NEXT_CONFIRMATION ->
                ReassessmentTransition.Promoted(state.nextLevel, outcome.criteria, nowMillis)
        }
    }
}
