package com.project.speakmate.data.local

import android.content.Context
import android.content.SharedPreferences
import com.project.speakmate.domain.model.AppLanguage
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageLocalDataSourceTest {
    @Test
    fun getSavedLanguageCachesInMemoryAfterFirstReadAndUpdatesOnSave() = runTest {
        val editor = mockk<SharedPreferences.Editor>()
        val preferences = mockk<SharedPreferences>()
        val context = mockk<Context>()
        every { context.getSharedPreferences("app_language", Context.MODE_PRIVATE) } returns preferences
        every { preferences.getString("language_tag", null) } returns AppLanguage.ENGLISH.languageTag
        every { preferences.edit() } returns editor
        every { editor.putString("language_tag", AppLanguage.VIETNAMESE.languageTag) } returns editor
        every { editor.commit() } returns true

        val dataSource = AppLanguageLocalDataSource(context)

        assertEquals(AppLanguage.ENGLISH, dataSource.getSavedLanguage())
        assertEquals(AppLanguage.ENGLISH, dataSource.getSavedLanguage())
        verify(exactly = 1) { preferences.getString("language_tag", null) }

        dataSource.saveLanguage(AppLanguage.VIETNAMESE)

        assertEquals(AppLanguage.VIETNAMESE, dataSource.getSavedLanguage())
        verify(exactly = 1) { preferences.getString("language_tag", null) }
    }

    @Test
    fun saveLanguageKeepsPreviousCachedValueWhenCommitFails() = runTest {
        val editor = mockk<SharedPreferences.Editor>()
        val preferences = mockk<SharedPreferences>()
        val context = mockk<Context>()
        every { context.getSharedPreferences("app_language", Context.MODE_PRIVATE) } returns preferences
        every { preferences.getString("language_tag", null) } returns AppLanguage.ENGLISH.languageTag
        every { preferences.edit() } returns editor
        every { editor.putString("language_tag", AppLanguage.VIETNAMESE.languageTag) } returns editor
        every { editor.commit() } returns false

        val dataSource = AppLanguageLocalDataSource(context)
        assertEquals(AppLanguage.ENGLISH, dataSource.getSavedLanguage())

        var failed = false
        try {
            dataSource.saveLanguage(AppLanguage.VIETNAMESE)
        } catch (_: IOException) {
            failed = true
        }

        assertEquals(true, failed)
        assertEquals(AppLanguage.ENGLISH, dataSource.getSavedLanguage())
    }
}
