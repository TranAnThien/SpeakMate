package com.project.speakmate.di

import com.project.speakmate.data.repository.PronunciationRepositoryImpl
import com.project.speakmate.domain.repository.PronunciationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
abstract class PronunciationModule {
    @Binds
    @ViewModelScoped
    abstract fun bindPronunciationRepository(implementation: PronunciationRepositoryImpl): PronunciationRepository
}
