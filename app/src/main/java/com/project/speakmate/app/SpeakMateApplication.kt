package com.project.speakmate.app

import android.app.Application
import android.util.Log
import com.project.speakmate.data.audio.AssessmentAudioFileProvider
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class SpeakMateApplication : Application() {
    companion object {
        private const val CLEANUP_TAG = "AssessmentCleanup"
        private const val CLEANUP_FAILED = "Temporary assessment evidence cleanup failed"
    }

    @Inject
    lateinit var assessmentAudioFiles: Provider<AssessmentAudioFileProvider>

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                assessmentAudioFiles.get().clearPreviousProcessFiles()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                Log.w(CLEANUP_TAG, CLEANUP_FAILED)
            }
        }
    }
}
