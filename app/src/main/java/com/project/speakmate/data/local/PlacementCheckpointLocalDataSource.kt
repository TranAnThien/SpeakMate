package com.project.speakmate.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlacementCheckpointLocalDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFERENCES_NAME = "placement_checkpoint"
        private const val KEY_PREFIX = "placement_checkpoint_"
        private const val WRITE_FAILED = "Placement checkpoint write failed"
    }

    suspend fun getCheckpoint(userId: String): String? = withContext(Dispatchers.IO) {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        try {
            preferences.getString(KEY_PREFIX + userId, null)
        } catch (_: ClassCastException) {
            if (!preferences.edit().remove(KEY_PREFIX + userId).commit()) throw IOException(WRITE_FAILED)
            null
        }
    }

    suspend fun saveCheckpoint(userId: String, checkpointJson: String): Unit = withContext(Dispatchers.IO) {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        if (!preferences.edit().putString(KEY_PREFIX + userId, checkpointJson).commit()) throw IOException(WRITE_FAILED)
    }

    suspend fun clearCheckpoint(userId: String): Unit = withContext(Dispatchers.IO) {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        if (!preferences.edit().remove(KEY_PREFIX + userId).commit()) throw IOException(WRITE_FAILED)
    }
}
