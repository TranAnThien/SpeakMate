package com.project.speakmate.di

import com.project.speakmate.data.repository.CallRepositoryImpl
import com.project.speakmate.data.session.CallSessionMedia
import com.project.speakmate.data.session.CallSessionMediaAdapter
import com.project.speakmate.domain.repository.CallRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class CallModule {
    @Binds
    abstract fun bindCallRepository(implementation: CallRepositoryImpl): CallRepository

    @Binds
    abstract fun bindCallSessionMedia(implementation: CallSessionMediaAdapter): CallSessionMedia
}
