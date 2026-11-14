package com.project.speakmate.di

import android.content.Context
import com.project.speakmate.data.audio.AssessmentAudioFileProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AssessmentModule {
    private const val ASSESSMENT_DIRECTORY = "assessment"

    @Provides
    @Singleton
    fun provideAssessmentAudioFiles(@ApplicationContext context: Context): AssessmentAudioFileProvider =
        AssessmentAudioFileProvider(File(context.cacheDir, ASSESSMENT_DIRECTORY))
}
