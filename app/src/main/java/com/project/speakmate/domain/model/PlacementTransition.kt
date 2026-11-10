package com.project.speakmate.domain.model

sealed interface PlacementTransition {
    data class NextTask(val checkpoint: PlacementCheckpoint) : PlacementTransition

    data class RetryTask(
        val checkpoint: PlacementCheckpoint,
        val reason: InsufficientEvidenceReason
    ) : PlacementTransition

    data class Confirmed(val checkpoint: PlacementCheckpoint, val result: PlacementResult) : PlacementTransition

    data class Stopped(val reason: PlacementStopReason) : PlacementTransition
}
