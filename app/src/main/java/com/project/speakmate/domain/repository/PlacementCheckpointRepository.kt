package com.project.speakmate.domain.repository

import com.project.speakmate.domain.model.PlacementCheckpoint

interface PlacementCheckpointRepository {
    suspend fun getCheckpoint(userId: String): PlacementCheckpoint?
    suspend fun saveCheckpoint(checkpoint: PlacementCheckpoint)
    suspend fun clearCheckpoint(userId: String)
}
