package com.project.speakmate.data.remote.firebase

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

class CredentialSessionProvider @Inject constructor(@ApplicationContext context: Context) {
    companion object {
        private const val TAG = "CredentialSessionProvider"
        private const val CLEAR_FAILED = "Credential state could not be cleared"
    }

    private val credentialManager = CredentialManager.create(context)

    suspend fun clearSession() {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Log.w(TAG, CLEAR_FAILED)
        }
    }
}
