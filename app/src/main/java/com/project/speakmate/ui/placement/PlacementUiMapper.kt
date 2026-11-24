package com.project.speakmate.ui.placement

import com.project.speakmate.domain.model.AssessmentTask
import com.project.speakmate.domain.model.PlacementStep
import com.project.speakmate.domain.model.PlacementTransition
import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.PlacementResult
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.ui.placement.state.PlacementPhase
import com.project.speakmate.ui.placement.state.PlacementUiState
import com.project.speakmate.ui.placement.state.PlacementResultUiState
import com.project.speakmate.ui.placement.state.PlacementSkillUiState

object PlacementUiMapper {
    fun toNextTask(
        state: PlacementUiState,
        previousStep: PlacementStep,
        transition: PlacementTransition.NextTask,
        tasks: List<AssessmentTask>
    ): PlacementUiState =
        state.copy(
            phase = PlacementPhase.NEXT_TASK,
            nextTask = PlacementNextTaskMapper.classify(previousStep, transition.checkpoint.step),
            currentTask =
                tasks.singleOrNull { task ->
                    task.taskId == transition.checkpoint.currentTaskId
                },
            currentTaskNumber = transition.checkpoint.records.size + 1,
            taskRole = transition.checkpoint.step.role,
            scenario = null
        )

    fun toResult(
        result: PlacementResult,
        isSavingResult: Boolean,
        saveFailed: Boolean,
        isResultSaved: Boolean
    ): PlacementResultUiState =
        PlacementResultUiState(
            level = result.level,
            range = PlacementSkillUiState(result.feedback.range, result.speakingProfile.rangeMet),
            accuracy = PlacementSkillUiState(result.feedback.accuracy, result.speakingProfile.accuracyMet),
            fluency = PlacementSkillUiState(result.feedback.fluency, result.speakingProfile.fluencyMet),
            interaction = PlacementSkillUiState(result.feedback.interaction, result.speakingProfile.interactionMet),
            coherence = PlacementSkillUiState(result.feedback.coherence, result.speakingProfile.coherenceMet),
            isLowestLevelAssigned =
                result.level == SpeakingLevel.A1 &&
                    result.taskResult in listOf(AssessmentResult.PARTIAL, AssessmentResult.FAIL),
            isSavingResult = isSavingResult,
            saveFailed = saveFailed,
            isResultSaved = isResultSaved
        )
}
