package com.project.speakmate.di

import com.project.speakmate.data.repository.ScenarioRepositoryImpl
import com.project.speakmate.domain.repository.ScenarioRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ScenarioModule {
    @Binds
    abstract fun bindScenarioRepository(implementation: ScenarioRepositoryImpl): ScenarioRepository
}
