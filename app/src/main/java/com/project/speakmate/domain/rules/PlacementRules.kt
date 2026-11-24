package com.project.speakmate.domain.rules

import com.project.speakmate.domain.model.User
import com.project.speakmate.domain.model.AssessmentOutcome
import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.AssessmentTask
import com.project.speakmate.domain.model.InsufficientEvidenceReason
import com.project.speakmate.domain.model.PlacementCheckpoint
import com.project.speakmate.domain.model.PlacementFeedback
import com.project.speakmate.domain.model.PlacementResult
import com.project.speakmate.domain.model.PlacementRole
import com.project.speakmate.domain.model.PlacementStep
import com.project.speakmate.domain.model.PlacementStopReason
import com.project.speakmate.domain.model.PlacementTaskRecord
import com.project.speakmate.domain.model.PlacementTransition
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.utils.extension.defaultEmpty
import kotlin.random.Random

object PlacementRules {
    const val CHECKPOINT_TTL_MILLIS = 24 * 60 * 60 * 1_000L
    const val MAX_VALID_TASKS = 7
    const val MAX_TECHNICAL_RETRIES = 2

    fun canStart(profile: User): Boolean = profile.level == SpeakingLevel.NOT_ASSESSED

    fun getResultDelayMillis(checkpoint: PlacementCheckpoint, nowMillis: Long): Long =
        (checkpoint.expiresAtMillis - nowMillis).coerceIn(0L, PracticeCompletionRules.AUDIO_RETENTION_MILLIS)

    fun canSaveResult(profile: User, result: PlacementResult): Boolean {
        val isAlreadySaved =
            profile.level == result.level &&
                profile.lastAssessedAt?.time == result.assessedAtMillis &&
                profile.latestAssessmentCriteria == result.speakingProfile
        return isAlreadySaved || profile.level == SpeakingLevel.NOT_ASSESSED
    }

    fun start(userId: String, task: AssessmentTask, nowMillis: Long): PlacementCheckpoint {
        require(task.targetLevel == SpeakingLevel.A2)
        return PlacementCheckpoint(
            userId = userId,
            step = PlacementStep.INITIAL_A2,
            currentTaskId = task.taskId,
            records = emptyList(),
            candidateLevel = SpeakingLevel.NOT_ASSESSED,
            usedScenarioIds = emptyList(),
            technicalRetryCount = 0,
            createdAtMillis = nowMillis,
            expiresAtMillis = nowMillis + CHECKPOINT_TTL_MILLIS,
            pendingResult = null
        )
    }

    fun getNextStep(step: PlacementStep, result: AssessmentResult): PlacementStep? {
        val hasPassed = result == AssessmentResult.PASS
        return when (step) {
            PlacementStep.INITIAL_A2 ->
                when (result) {
                    AssessmentResult.PASS -> PlacementStep.B1_PROBE
                    AssessmentResult.PARTIAL -> PlacementStep.A2_CONFIRMATION
                    AssessmentResult.FAIL -> PlacementStep.A1_CONFIRMATION
                }
            PlacementStep.B1_PROBE -> if (hasPassed) PlacementStep.B2_PROBE else PlacementStep.A2_CONFIRMATION
            PlacementStep.B2_PROBE -> if (hasPassed) PlacementStep.B2_CONFIRMATION else PlacementStep.B1_CONFIRMATION
            PlacementStep.B2_CONFIRMATION -> if (hasPassed) null else PlacementStep.B1_CONFIRMATION
            PlacementStep.B1_CONFIRMATION -> if (hasPassed) null else PlacementStep.A2_CONFIRMATION
            PlacementStep.A2_CONFIRMATION -> if (hasPassed) null else PlacementStep.A1_CONFIRMATION
            PlacementStep.A1_CONFIRMATION -> null
        }
    }

    fun selectTask(
        step: PlacementStep,
        tasks: List<AssessmentTask>,
        checkpoint: PlacementCheckpoint?,
        random: Random
    ): AssessmentTask? {
        if (checkpoint != null && checkpoint.step == step && checkpoint.currentTaskId != null) {
            return tasks.firstOrNull { it.taskId == checkpoint.currentTaskId && it.targetLevel == step.level }
        }
        val excludedScenarios =
            if (step.role == PlacementRole.CONFIRMATION) {
                checkpoint?.records.defaultEmpty().filter { it.step.level == step.level }.map { it.scenarioId }.toSet()
            } else {
                checkpoint?.usedScenarioIds.defaultEmpty().toSet()
            }
        return tasks
            .filter { it.targetLevel == step.level && it.scenarioId !in excludedScenarios }
            .shuffled(random)
            .firstOrNull()
    }

    fun applyOutcome(
        checkpoint: PlacementCheckpoint,
        task: AssessmentTask,
        outcome: AssessmentOutcome,
        tasks: List<AssessmentTask>,
        random: Random,
        nowMillis: Long
    ): PlacementTransition {
        require(task.taskId == checkpoint.currentTaskId && task.targetLevel == checkpoint.step.level)
        return when (outcome) {
            is AssessmentOutcome.InsufficientEvidence -> applyInsufficientEvidence(checkpoint, outcome.reason)
            is AssessmentOutcome.Evaluated ->
                applyEvaluated(
                    checkpoint = checkpoint,
                    task = task,
                    outcome = outcome,
                    tasks = tasks,
                    random = random,
                    nowMillis = nowMillis
                )
        }
    }

    fun isExpired(checkpoint: PlacementCheckpoint, nowMillis: Long): Boolean {
        if (nowMillis < checkpoint.createdAtMillis) return true
        val elapsedMillis = nowMillis.toULong() - checkpoint.createdAtMillis.toULong()
        if (elapsedMillis >= CHECKPOINT_TTL_MILLIS.toULong()) return true
        return checkpoint.expiresAtMillis > 0L && nowMillis >= checkpoint.expiresAtMillis
    }

    private fun applyInsufficientEvidence(
        checkpoint: PlacementCheckpoint,
        reason: InsufficientEvidenceReason
    ): PlacementTransition {
        if (checkpoint.technicalRetryCount >= MAX_TECHNICAL_RETRIES) {
            return PlacementTransition.Stopped(PlacementStopReason.TECHNICAL_RETRY_LIMIT)
        }
        return PlacementTransition.RetryTask(
            checkpoint.copy(technicalRetryCount = checkpoint.technicalRetryCount + 1),
            reason
        )
    }

    private fun applyEvaluated(
        checkpoint: PlacementCheckpoint,
        task: AssessmentTask,
        outcome: AssessmentOutcome.Evaluated,
        tasks: List<AssessmentTask>,
        random: Random,
        nowMillis: Long
    ): PlacementTransition {
        if (checkpoint.records.size >= MAX_VALID_TASKS)
            return PlacementTransition.Stopped(PlacementStopReason.TASK_LIMIT_REACHED)
        val record =
            PlacementTaskRecord(
                step = checkpoint.step,
                taskId = task.taskId,
                scenarioId = task.scenarioId,
                result = outcome.result,
                criteria = outcome.criteria
            )
        val nextStep = getNextStep(checkpoint.step, outcome.result)
        val updated =
            checkpoint.copy(
                currentTaskId = null,
                records = checkpoint.records + record,
                usedScenarioIds = (checkpoint.usedScenarioIds + task.scenarioId).distinct(),
                technicalRetryCount = 0
            )
        if (nextStep == null) {
            val result =
                PlacementResult(
                    level = checkpoint.step.level,
                    speakingProfile = outcome.criteria,
                    assessedAtMillis = nowMillis,
                    taskResult = outcome.result,
                    feedback =
                        PlacementFeedback(
                            range = outcome.evaluation.range.feedback,
                            accuracy = outcome.evaluation.accuracy.feedback,
                            fluency = outcome.evaluation.fluency.feedback,
                            interaction = outcome.evaluation.interaction.feedback,
                            coherence = outcome.evaluation.coherence.feedback
                        )
                )
            return PlacementTransition.Confirmed(
                updated.copy(candidateLevel = result.level, pendingResult = result),
                result
            )
        }
        if (updated.records.size >= MAX_VALID_TASKS)
            return PlacementTransition.Stopped(PlacementStopReason.TASK_LIMIT_REACHED)
        val candidateLevel =
            when {
                nextStep.role == PlacementRole.CONFIRMATION -> nextStep.level
                outcome.result == AssessmentResult.PASS && checkpoint.step.level > checkpoint.candidateLevel ->
                    checkpoint.step.level
                else -> checkpoint.candidateLevel
            }
        val nextCheckpoint = updated.copy(step = nextStep, candidateLevel = candidateLevel)
        val nextTask =
            selectTask(step = nextStep, tasks = tasks, checkpoint = nextCheckpoint, random = random)
                ?: return PlacementTransition.Stopped(PlacementStopReason.NO_TASK_AVAILABLE)
        return PlacementTransition.NextTask(nextCheckpoint.copy(currentTaskId = nextTask.taskId))
    }
}
