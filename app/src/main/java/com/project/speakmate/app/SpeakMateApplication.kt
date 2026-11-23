package com.project.speakmate.app

import android.app.Application
import android.util.Log
import com.project.speakmate.data.audio.AssessmentAudioFileProvider
import com.project.speakmate.data.remote.NetworkStatusProvider
import com.project.speakmate.di.ApplicationScope
import com.project.speakmate.domain.repository.PracticeCompletionRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch

@HiltAndroidApp
class SpeakMateApplication : Application() {
    companion object {
        private const val NETWORK_RETRY_DELAY_MILLIS = 1_000L
        private const val CLEANUP_TAG = "AssessmentCleanup"
        private const val CLEANUP_FAILED = "Temporary assessment evidence cleanup failed"
    }

    @Inject
    lateinit var assessmentAudioFiles: Provider<AssessmentAudioFileProvider>

    @Inject
    lateinit var completionRepository: Provider<PracticeCompletionRepository>

    @Inject
    lateinit var networkStatus: Provider<NetworkStatusProvider>

    @Inject
    @field:ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            networkStatus.get().observeConnected().retryWhen { failure, _ ->
                if (failure is CancellationException) throw failure
                completionRepository.get().setNetworkAvailable(false)
                delay(NETWORK_RETRY_DELAY_MILLIS)
                true
            }.collect { completionRepository.get().setNetworkAvailable(it) }
        }
        applicationScope.launch(Dispatchers.IO) {
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
