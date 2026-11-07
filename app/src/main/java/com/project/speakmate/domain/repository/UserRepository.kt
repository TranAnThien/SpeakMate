package com.project.speakmate.domain.repository

import com.project.speakmate.domain.model.User

interface UserRepository {
    suspend fun getUser(userId: String): Result<User?>
    suspend fun getOrCreateUser(user: User): Result<User>
}