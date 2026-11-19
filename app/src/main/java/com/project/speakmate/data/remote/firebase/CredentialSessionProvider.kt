package com.project.speakmate.data.remote.firebase

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.ClearCredentialException
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class CredentialSessionProvider @Inject constructor(
    @ApplicationContext context: Context
) {
    companion object {
        private const val TAG = "CredentialSessionProvider"
    }

    private val credentialManager = CredentialManager.create(context)

    suspend fun clearSession() {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (_: ClearCredentialException) {
            Log.w(TAG, "Credential state could not be cleared")
        }
    }
}
