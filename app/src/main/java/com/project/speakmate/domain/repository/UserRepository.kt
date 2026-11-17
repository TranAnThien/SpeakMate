package com.project.speakmate.domain.repository

import com.project.speakmate.domain.model.SpeakingCriteriaResult
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingProgress
import com.project.speakmate.domain.model.User

interface UserRepository {
    suspend fun getUser(userId: String): Result<User?>
    suspend fun getOrCreateUser(user: User): Result<User>
    suspend fun updateUserLevelAndProgress(
        newLevel: SpeakingLevel,
        criteriaResult: SpeakingCriteriaResult,
        newProgress: TrainingProgress
    ): Result<Unit>
}