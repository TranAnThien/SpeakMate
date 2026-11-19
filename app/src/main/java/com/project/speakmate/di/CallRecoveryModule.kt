package com.project.speakmate.di

import com.project.speakmate.data.repository.CallRecoveryRepositoryImpl
import com.project.speakmate.domain.repository.CallRecoveryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class CallRecoveryModule {
    @Binds
    abstract fun bindCallRecoveryRepository(implementation: CallRecoveryRepositoryImpl): CallRecoveryRepository
}
