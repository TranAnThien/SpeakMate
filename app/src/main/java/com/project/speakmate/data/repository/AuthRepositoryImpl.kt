package com.project.speakmate.data.repository

import com.project.speakmate.data.remote.firebase.FirebaseAuthDataSource
import com.project.speakmate.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authDataSource: FirebaseAuthDataSource
) : AuthRepository {

    override suspend fun signInWithGoogle(idToken: String): Result<String> {
        return try {
            val uid = authDataSource.signInWithGoogle(idToken)
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            authDataSource.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getSignedInUserId(): Flow<String?> {
        return authDataSource.getSignedInUserId()
    }
}
