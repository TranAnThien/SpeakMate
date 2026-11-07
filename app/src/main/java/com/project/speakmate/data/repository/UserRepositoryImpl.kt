package com.project.speakmate.data.repository

import com.project.speakmate.data.dto.toDomain
import com.project.speakmate.data.dto.toDto
import com.project.speakmate.data.remote.firebase.FirestoreUserDataSource
import com.project.speakmate.domain.model.User
import com.project.speakmate.domain.repository.UserRepository

class UserRepositoryImpl(
    private val firestoreUserDataSource: FirestoreUserDataSource = FirestoreUserDataSource()
) : UserRepository {

    override suspend fun getOrCreateUser(user: User): Result<User> {
        return try {
            val savedDto = firestoreUserDataSource.getOrCreateUser(user.toDto())
            Result.success(savedDto.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUser(userId: String): Result<User?> {
        return try {
            val userDto = firestoreUserDataSource.getUser(userId)
            Result.success(userDto?.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}