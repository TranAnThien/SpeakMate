package com.project.speakmate.di

import com.project.speakmate.BuildConfig
import com.project.speakmate.utils.AppConstant
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val SOCKET_PING_SECONDS = 10L

    @Provides
    @Singleton
    fun provideHttpClient(): OkHttpClient =
        OkHttpClient.Builder().pingInterval(SOCKET_PING_SECONDS, TimeUnit.SECONDS).build()

    @Provides
    @Named(AppConstant.GEMINI_API_KEY)
    fun provideGeminiApiKey(): String = BuildConfig.GEMINI_API_KEY
}
