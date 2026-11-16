package com.project.speakmate.domain.model

sealed interface TrainingProgressDecision {
    data class Advanced(val progress: TrainingProgress, val reason: AdvanceReason) : TrainingProgressDecision

    data class Unchanged(val reason: ProgressHoldReason) : TrainingProgressDecision

    data class PathCompleted(val progress: TrainingProgress) : TrainingProgressDecision

    data class Invalid(val reason: ProgressProblem) : TrainingProgressDecision

    enum class AdvanceReason {
        NEXT_UNIT_OPENED,
        GOAL_ASSESSMENT_OPENED,
        NEXT_GOAL_OPENED
    }
}
