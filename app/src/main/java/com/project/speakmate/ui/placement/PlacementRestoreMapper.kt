package com.project.speakmate.ui.placement

import com.project.speakmate.domain.model.PlacementCheckpoint
import com.project.speakmate.domain.rules.PlacementRules
import com.project.speakmate.ui.placement.state.PlacementPhase
import com.project.speakmate.ui.placement.state.PlacementRestoreResult

object PlacementRestoreMapper {
    fun restore(checkpoint: PlacementCheckpoint?, nowMillis: Long): PlacementRestoreResult {
        if (checkpoint == null) return PlacementRestoreResult(PlacementPhase.INTRODUCTION, null, false)
        if (PlacementRules.isExpired(checkpoint, nowMillis)) {
            return PlacementRestoreResult(PlacementPhase.INTRODUCTION, null, true)
        }
        val phase = if (checkpoint.pendingResult != null) PlacementPhase.RESULT else PlacementPhase.RESUME_PROMPT
        return PlacementRestoreResult(phase, checkpoint, false)
    }
}
