package com.project.speakmate.di

import com.project.speakmate.data.repository.AuthRepositoryImpl
import com.project.speakmate.data.repository.UserRepositoryImpl
import com.project.speakmate.domain.repository.AuthRepository
import com.project.speakmate.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class UserModule {
    @Binds
    abstract fun bindAuthRepository(implementation: AuthRepositoryImpl): AuthRepository

    @Binds
    abstract fun bindUserRepository(implementation: UserRepositoryImpl): UserRepository
}
