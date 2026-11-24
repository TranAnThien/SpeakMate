package com.project.speakmate.domain.repository

import com.project.speakmate.domain.model.AppLanguage
import kotlinx.coroutines.flow.Flow

interface AppLanguageRepository {
    fun observeLanguage(): Flow<AppLanguage>

    suspend fun saveLanguage(language: AppLanguage): Result<Unit>
}
