package com.project.speakmate.data.local

import android.content.Context
import com.project.speakmate.domain.model.AppLanguage
import com.project.speakmate.utils.extension.default
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class AppLanguageLocalDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFERENCES_NAME = "app_language"
        private const val LANGUAGE_TAG = "language_tag"
        private const val WRITE_FAILED = "App language write failed"
    }

    @Volatile private var cachedLanguage: AppLanguage? = null

    suspend fun getLanguage(): AppLanguage = withContext(Dispatchers.IO) {
        getSavedLanguage()
    }

    fun getSavedLanguage(): AppLanguage {
        cachedLanguage?.let { return it }
        return readSavedLanguage().also { cachedLanguage = it }
    }

    fun readSavedLanguage(): AppLanguage {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val tag = try { preferences.getString(LANGUAGE_TAG, null) } catch (_: ClassCastException) { null }
        return AppLanguage.entries.firstOrNull { it.languageTag == tag }.default(AppLanguage.VIETNAMESE)
    }

    suspend fun saveLanguage(language: AppLanguage): Unit = withContext(Dispatchers.IO) {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        if (!preferences.edit().putString(LANGUAGE_TAG, language.languageTag).commit()) throw IOException(WRITE_FAILED)
        cachedLanguage = language
    }
}
