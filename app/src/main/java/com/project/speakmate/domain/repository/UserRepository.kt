package com.project.speakmate.domain.repository

import com.project.speakmate.domain.model.User
import com.project.speakmate.domain.model.SpeakingCriteriaResult
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingProgress
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getUser(userId: String, requireServer: Boolean = false): Result<User?>
    suspend fun getOrCreateUser(user: User): Result<User>
    fun observeUser(userId: String): Flow<User?>
    suspend fun saveAssessedLevel(
        level: SpeakingLevel,
        criteria: SpeakingCriteriaResult,
        assessedAtMillis: Long,
        trainingProgress: TrainingProgress
    ): Result<Unit>
    suspend fun markNotAssessed(): Result<Unit>
}
