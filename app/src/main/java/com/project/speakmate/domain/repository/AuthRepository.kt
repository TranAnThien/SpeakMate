package com.project.speakmate.domain.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun signInWithGoogle(idToken: String): Result<String>
    suspend fun signOut(): Result<Unit>
    fun getSignedInUserId(): Flow<String?>
}