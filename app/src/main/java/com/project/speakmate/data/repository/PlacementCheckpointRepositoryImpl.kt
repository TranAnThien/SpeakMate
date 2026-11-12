package com.project.speakmate.data.repository

import com.project.speakmate.data.dto.PlacementCheckpointDto
import com.project.speakmate.data.local.PlacementCheckpointLocalDataSource
import com.project.speakmate.domain.model.PlacementCheckpoint
import com.project.speakmate.domain.repository.PlacementCheckpointRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlacementCheckpointRepositoryImpl @Inject constructor(
    private val localDataSource: PlacementCheckpointLocalDataSource
) : PlacementCheckpointRepository {
    override suspend fun getCheckpoint(userId: String): PlacementCheckpoint? = withContext(Dispatchers.IO) {
        val json = localDataSource.getCheckpoint(userId) ?: return@withContext null
        val checkpoint = PlacementCheckpointDto(json).toPlacementCheckpoint()
        if (checkpoint == null || checkpoint.userId != userId) {
            localDataSource.clearCheckpoint(userId)
            return@withContext null
        }
        checkpoint
    }

    override suspend fun saveCheckpoint(checkpoint: PlacementCheckpoint): Unit = withContext(Dispatchers.IO) {
        localDataSource.saveCheckpoint(checkpoint.userId, PlacementCheckpointDto.fromCheckpoint(checkpoint).toJson())
    }

    override suspend fun clearCheckpoint(userId: String) {
        localDataSource.clearCheckpoint(userId)
    }
}
