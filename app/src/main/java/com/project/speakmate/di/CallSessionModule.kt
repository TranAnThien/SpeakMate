package com.project.speakmate.di

import android.os.SystemClock
import com.project.speakmate.data.local.ActiveCallLocalDataSource
import com.project.speakmate.data.remote.firebase.FirebaseAuthDataSource
import com.project.speakmate.data.session.CallSessionManager
import com.project.speakmate.data.session.CallSessionMedia
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Provider
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(ViewModelComponent::class)
object CallSessionModule {
    @Provides
    @ViewModelScoped
    fun provideCallSessionManager(
        media: Provider<CallSessionMedia>,
        markers: ActiveCallLocalDataSource,
        authDataSource: FirebaseAuthDataSource
    ): CallSessionManager {
        val userId = try {
            authDataSource.requireCurrentUserId()
        } catch (_: IllegalStateException) {
            null
        }
        return CallSessionManager(
            media::get, Dispatchers.IO.limitedParallelism(1), SystemClock::elapsedRealtime,
            saveActiveMarker = { owner -> userId?.let { markers.saveActiveCall(it, owner) } },
            removeActiveMarker = markers::removeActiveCall
        )
    }
}
