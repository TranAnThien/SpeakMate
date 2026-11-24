package com.project.speakmate.data.repository

import com.project.speakmate.data.dto.toDomain
import com.project.speakmate.data.dto.toDto
import com.project.speakmate.data.remote.firebase.FirestoreUserDataSource
import com.project.speakmate.domain.model.SpeakingCriteriaResult
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingProgress
import com.project.speakmate.domain.model.User
import com.project.speakmate.domain.repository.UserRepository
import com.project.speakmate.utils.extension.suspendRunCatching
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserRepositoryImpl @Inject constructor(private val firestoreUserDataSource: FirestoreUserDataSource) :
    UserRepository {

    override suspend fun getOrCreateUser(user: User): Result<User> = suspendRunCatching {
        val savedDto = firestoreUserDataSource.getOrCreateUser(user.toDto())
        savedDto.toDomain()
    }

    override suspend fun getUser(userId: String, requireServer: Boolean): Result<User?> = suspendRunCatching {
        val userDto = firestoreUserDataSource.getUser(userId, requireServer)
        userDto?.toDomain()
    }

    override fun observeUser(userId: String): Flow<User?> =
        firestoreUserDataSource.observeUser(userId).map { it?.toDomain() }

    override suspend fun saveAssessedLevel(
        level: SpeakingLevel,
        criteria: SpeakingCriteriaResult,
        assessedAtMillis: Long,
        trainingProgress: TrainingProgress
    ): Result<Unit> = suspendRunCatching {
        require(level != SpeakingLevel.NOT_ASSESSED)
        firestoreUserDataSource.saveAssessedLevel(
            level = level.name,
            criteria = criteria.toDto(),
            assessedAt = Date(assessedAtMillis),
            trainingProgress = trainingProgress.toDto()
        )
    }

    override suspend fun markNotAssessed(): Result<Unit> = suspendRunCatching {
        firestoreUserDataSource.markNotAssessed()
    }
}
