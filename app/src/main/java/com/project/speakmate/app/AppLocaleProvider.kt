package com.project.speakmate.app

import android.content.Context
import android.content.res.Configuration
import com.project.speakmate.data.local.AppLanguageLocalDataSource
import java.util.Locale

class AppLocaleProvider {
    fun wrap(base: Context): Context {
        val language = AppLanguageLocalDataSource(base).getSavedLanguage()
        val locale = Locale.forLanguageTag(language.languageTag)
        Locale.setDefault(locale)
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(locale)
        return base.createConfigurationContext(configuration)
    }
}
