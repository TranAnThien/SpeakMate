package com.project.speakmate.test

import com.project.speakmate.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAuthRepository(userId: String? = "user-1") : AuthRepository {
    var isConnected = true

    override fun isNetworkAvailable(): Boolean = isConnected

    val signedInUserId = MutableStateFlow(userId)

    override suspend fun signInWithGoogle(idToken: String): Result<String> = Result.success(requireCurrentUserId())

    override suspend fun signOut(): Result<Unit> {
        signedInUserId.value = null
        return Result.success(Unit)
    }

    override fun observeSignedInUserId(): Flow<String?> = signedInUserId

    override fun requireCurrentUserId(): String = checkNotNull(signedInUserId.value)
}
