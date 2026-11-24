package com.project.speakmate.data.repository

import com.project.speakmate.data.local.ActiveCallLocalDataSource
import com.project.speakmate.data.remote.NetworkStatusProvider
import com.project.speakmate.data.remote.firebase.FirebaseAuthDataSource
import com.project.speakmate.domain.repository.AuthRepository
import com.project.speakmate.utils.extension.suspendRunCatching
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class AuthRepositoryImpl
@Inject
constructor(
    private val authDataSource: FirebaseAuthDataSource,
    private val callMarkers: ActiveCallLocalDataSource,
    private val networkStatus: NetworkStatusProvider
) : AuthRepository {

    override suspend fun signInWithGoogle(idToken: String): Result<String> = suspendRunCatching {
        val uid = authDataSource.signInWithGoogle(idToken)
        uid
    }

    override suspend fun signOut(): Result<Unit> = suspendRunCatching {
        val userId =
            try {
                authDataSource.requireCurrentUserId()
            } catch (_: IllegalStateException) {
                null
            }
        authDataSource.signOut()
        userId?.let(callMarkers::removeUserCall)
        Unit
    }

    override fun observeSignedInUserId(): Flow<String?> {
        return authDataSource.observeSignedInUserId()
    }

    override fun isNetworkAvailable(): Boolean = networkStatus.isConnected()

    override fun requireCurrentUserId(): String = authDataSource.requireCurrentUserId()
}
