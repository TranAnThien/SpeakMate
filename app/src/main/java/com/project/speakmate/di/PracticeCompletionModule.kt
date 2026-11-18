package com.project.speakmate.di

import com.project.speakmate.data.repository.PracticeCompletionRepositoryImpl
import com.project.speakmate.domain.repository.PracticeCompletionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
abstract class PracticeCompletionModule {
    companion object {
        @Provides
        @Singleton
        @ApplicationScope
        fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    @Binds
    @Singleton
    abstract fun bindPracticeCompletionRepository(implementation: PracticeCompletionRepositoryImpl): PracticeCompletionRepository
}
