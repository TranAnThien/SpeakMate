package com.project.speakmate.di

import com.project.speakmate.data.repository.AssessmentTaskRepositoryImpl
import com.project.speakmate.data.repository.PlacementCheckpointRepositoryImpl
import com.project.speakmate.data.repository.SpeakingAssessmentRepositoryImpl
import com.project.speakmate.domain.repository.AssessmentTaskRepository
import com.project.speakmate.domain.repository.PlacementCheckpointRepository
import com.project.speakmate.domain.repository.SpeakingAssessmentRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class PlacementModule {
    @Binds
    abstract fun bindPlacementCheckpointRepository(implementation: PlacementCheckpointRepositoryImpl): PlacementCheckpointRepository

    @Binds
    abstract fun bindAssessmentTaskRepository(implementation: AssessmentTaskRepositoryImpl): AssessmentTaskRepository

    @Binds
    abstract fun bindSpeakingAssessmentRepository(implementation: SpeakingAssessmentRepositoryImpl): SpeakingAssessmentRepository
}
