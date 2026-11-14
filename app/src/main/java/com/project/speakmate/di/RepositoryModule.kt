package com.project.speakmate.di

import com.project.speakmate.data.repository.DictionaryRepositoryImpl
import com.project.speakmate.data.repository.SavedVocabularyRepositoryImpl
import com.project.speakmate.domain.repository.DictionaryRepository
import com.project.speakmate.domain.repository.SavedVocabularyRepository
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
}