package com.project.speakmate.data.repository

import kotlinx.coroutines.CancellationException

import com.project.speakmate.data.dto.toDomain
import com.project.speakmate.data.dto.toDto
import com.project.speakmate.data.remote.firebase.FirestoreUserDataSource
import com.project.speakmate.domain.model.User
import com.project.speakmate.domain.model.SpeakingCriteriaResult
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingProgress
import com.project.speakmate.domain.repository.UserRepository
import com.project.speakmate.utils.extension.suspendRunCatching
import java.util.Date
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firestoreUserDataSource: FirestoreUserDataSource
) : UserRepository {

    override suspend fun getOrCreateUser(user: User): Result<User> {
        return try {
            val savedDto = firestoreUserDataSource.getOrCreateUser(user.toDto())
            Result.success(savedDto.toDomain())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUser(userId: String, requireServer: Boolean): Result<User?> {
        return try {
            val userDto = firestoreUserDataSource.getUser(userId, requireServer)
            Result.success(userDto?.toDomain())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            Result.failure(e)
        }
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
        firestoreUserDataSource.saveAssessedLevel(level.name, criteria.toDto(), Date(assessedAtMillis), trainingProgress.toDto())
    }

    override suspend fun markNotAssessed(): Result<Unit> = suspendRunCatching {
        firestoreUserDataSource.markNotAssessed()
    }
}
