package com.project.speakmate.data.repository

import com.project.speakmate.data.local.ActiveCallLocalDataSource
import kotlinx.coroutines.CancellationException

import com.project.speakmate.data.remote.firebase.FirebaseAuthDataSource
import com.project.speakmate.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authDataSource: FirebaseAuthDataSource,
    private val callMarkers: ActiveCallLocalDataSource
) : AuthRepository {

    override suspend fun signInWithGoogle(idToken: String): Result<String> {
        return try {
            val uid = authDataSource.signInWithGoogle(idToken)
            Result.success(uid)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            val userId = try { authDataSource.requireCurrentUserId() } catch (_: IllegalStateException) { null }
            authDataSource.signOut()
            userId?.let(callMarkers::removeUserCall)
            Result.success(Unit)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeSignedInUserId(): Flow<String?> {
        return authDataSource.observeSignedInUserId()
    }

    override fun requireCurrentUserId(): String = authDataSource.requireCurrentUserId()
}
