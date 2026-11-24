package com.project.speakmate.di

import com.project.speakmate.data.repository.AssessmentGradingRepositoryImpl
import com.project.speakmate.domain.repository.AssessmentGradingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
abstract class AssessmentGradingModule {
    @Binds
    @ViewModelScoped
    abstract fun bindAssessmentGradingRepository(implementation: AssessmentGradingRepositoryImpl): AssessmentGradingRepository
}
