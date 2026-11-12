package com.project.speakmate.di

import com.project.speakmate.data.repository.PracticeRepositoryImpl
import com.project.speakmate.data.repository.TrainingRepositoryImpl
import com.project.speakmate.domain.repository.PracticeRepository
import com.project.speakmate.domain.repository.TrainingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TrainingDataBindingsModule {
    @Binds
    @Singleton
    abstract fun bindTrainingRepository(implementation: TrainingRepositoryImpl): TrainingRepository

    @Binds
    @Singleton
    abstract fun bindPracticeRepository(implementation: PracticeRepositoryImpl): PracticeRepository
}
