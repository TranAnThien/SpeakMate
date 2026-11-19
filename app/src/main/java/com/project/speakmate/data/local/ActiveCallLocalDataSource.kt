package com.project.speakmate.data.local

import android.content.Context
import com.project.speakmate.data.remote.firebase.FirebaseAuthDataSource
import com.project.speakmate.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.json.JSONObject

@Singleton
class ActiveCallLocalDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope scope: CoroutineScope,
    private val authDataSource: FirebaseAuthDataSource
) {
    companion object {
        private const val PREFERENCES_NAME = "active_call"
        private const val USER_PREFIX = "active_user_"
        private const val OWNER_FIELD = "owner"
        private const val PROCESS_FIELD = "process"
    }

    private val processId = UUID.randomUUID().toString()
    private val preferences by lazy { context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE) }
    private val commands = Channel<() -> Unit>(Channel.UNLIMITED)

    init {
        scope.launch {
            try {
                for (command in commands) {
                    try {
                        command()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        Unit
                    }
                }
            } finally {
                commands.close()
            }
        }
    }

    fun saveActiveCall(userId: String, ownerId: String) {
        commands.trySend {
            if (currentUserId() == userId) {
                val marker = JSONObject().put(OWNER_FIELD, ownerId).put(PROCESS_FIELD, processId).toString()
                preferences.edit().putString(USER_PREFIX + userId, marker).commit()
            }
        }
    }

    fun removeActiveCall(ownerId: String) {
        commands.trySend {
            val editor = preferences.edit()
            preferences.all.forEach { (key, value) ->
                if (key.startsWith(USER_PREFIX) && value is String && JSONObject(value).optString(OWNER_FIELD) == ownerId) editor.remove(key)
            }
            editor.commit()
        }
    }

    fun removeUserCall(userId: String) {
        commands.trySend { preferences.edit().remove(USER_PREFIX + userId).commit() }
    }

    suspend fun consumeInterruptedCall(userId: String): Boolean {
        val result = CompletableDeferred<Boolean>()
        val sent = commands.trySend {
            try {
                val key = USER_PREFIX + userId
                val value = preferences.getString(key, null)
                val isInterrupted = currentUserId() == userId && value != null && JSONObject(value).optString(PROCESS_FIELD) != processId
                if (isInterrupted) preferences.edit().remove(key).commit()
                result.complete(isInterrupted)
            } catch (cancelled: CancellationException) {
                result.complete(false)
                throw cancelled
            } catch (_: Exception) {
                result.complete(false)
            }
        }
        return if (sent.isSuccess) result.await() else false
    }

    private fun currentUserId(): String? = try {
        authDataSource.requireCurrentUserId()
    } catch (_: IllegalStateException) {
        null
    }
}
