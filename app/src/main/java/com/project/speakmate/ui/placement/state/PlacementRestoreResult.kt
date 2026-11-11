package com.project.speakmate.ui.placement.state

import com.project.speakmate.domain.model.PlacementCheckpoint

data class PlacementRestoreResult(
    val phase: PlacementPhase,
    val checkpoint: PlacementCheckpoint?,
    val shouldClearCheckpoint: Boolean
)
