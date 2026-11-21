package com.project.speakmate.di

import com.project.speakmate.data.repository.AssessmentTaskRepositoryImpl
import com.project.speakmate.data.repository.AuthRepositoryImpl
import com.project.speakmate.data.repository.CallRecoveryRepositoryImpl
import com.project.speakmate.data.repository.DictionaryRepositoryImpl
import com.project.speakmate.data.repository.HistoryRepositoryImpl
import com.project.speakmate.data.repository.PlacementCheckpointRepositoryImpl
import com.project.speakmate.data.repository.PracticeCompletionRepositoryImpl
import com.project.speakmate.data.repository.PracticeRepositoryImpl
import com.project.speakmate.data.repository.SavedVocabularyRepositoryImpl
import com.project.speakmate.data.repository.ScenarioRepositoryImpl
import com.project.speakmate.data.repository.SpeakingAssessmentRepositoryImpl
import com.project.speakmate.data.repository.TrainingRepositoryImpl
import com.project.speakmate.data.repository.UserRepositoryImpl
import com.project.speakmate.domain.repository.AssessmentTaskRepository
import com.project.speakmate.domain.repository.AuthRepository
import com.project.speakmate.domain.repository.CallRecoveryRepository
import com.project.speakmate.domain.repository.DictionaryRepository
import com.project.speakmate.domain.repository.HistoryRepository
import com.project.speakmate.domain.repository.PlacementCheckpointRepository
import com.project.speakmate.domain.repository.PracticeCompletionRepository
import com.project.speakmate.domain.repository.PracticeRepository
import com.project.speakmate.domain.repository.SavedVocabularyRepository
import com.project.speakmate.domain.repository.ScenarioRepository
import com.project.speakmate.domain.repository.SpeakingAssessmentRepository
import com.project.speakmate.domain.repository.TrainingRepository
import com.project.speakmate.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSavedVocabularyRepository(
        impl: SavedVocabularyRepositoryImpl
    ): SavedVocabularyRepository

    @Binds
    @Singleton
    abstract fun bindDictionaryRepository(
        impl: DictionaryRepositoryImpl
    ): DictionaryRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(
        impl: HistoryRepositoryImpl
    ): HistoryRepository

    @Binds
    abstract fun bindAuthRepository(implementation: AuthRepositoryImpl): AuthRepository

    @Binds
    abstract fun bindUserRepository(implementation: UserRepositoryImpl): UserRepository

    @Binds
    abstract fun bindScenarioRepository(implementation: ScenarioRepositoryImpl): ScenarioRepository

    @Binds
    abstract fun bindCallRecoveryRepository(implementation: CallRecoveryRepositoryImpl): CallRecoveryRepository

    @Binds
    abstract fun bindPlacementCheckpointRepository(implementation: PlacementCheckpointRepositoryImpl): PlacementCheckpointRepository

    @Binds
    abstract fun bindAssessmentTaskRepository(implementation: AssessmentTaskRepositoryImpl): AssessmentTaskRepository

    @Binds
    abstract fun bindSpeakingAssessmentRepository(implementation: SpeakingAssessmentRepositoryImpl): SpeakingAssessmentRepository

    @Binds
    @Singleton
    abstract fun bindTrainingRepository(implementation: TrainingRepositoryImpl): TrainingRepository

    @Binds
    @Singleton
    abstract fun bindPracticeRepository(implementation: PracticeRepositoryImpl): PracticeRepository

    @Binds
    @Singleton
    abstract fun bindPracticeCompletionRepository(implementation: PracticeCompletionRepositoryImpl): PracticeCompletionRepository
}
