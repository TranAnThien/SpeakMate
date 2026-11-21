package com.project.speakmate.ui.placement

import com.project.speakmate.domain.model.PlacementCheckpoint
import com.project.speakmate.domain.repository.PlacementCheckpointRepository

class FakePlacementCheckpointRepository(
    var saved: PlacementCheckpoint? = null
) : PlacementCheckpointRepository {
    var clearCalls = 0

    override suspend fun getCheckpoint(userId: String): PlacementCheckpoint? {
        return saved?.takeIf { it.userId == userId }
    }

    override suspend fun saveCheckpoint(checkpoint: PlacementCheckpoint) {
        saved = checkpoint
    }

    override suspend fun clearCheckpoint(userId: String) {
        clearCalls++
        saved = null
    }
}
