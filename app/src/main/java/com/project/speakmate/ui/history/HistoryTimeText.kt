package com.project.speakmate.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun rememberHistoryDate(epochMillis: Long?): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(epochMillis, locale) {
        if (epochMillis == null) "" else DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
            .format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
    }
}

@Composable
fun rememberHistoryTime(epochMillis: Long?): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(epochMillis, locale) {
        if (epochMillis == null) "" else DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
            .format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
    }
}

@Composable
fun rememberHistoryDateTime(epochMillis: Long?): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(epochMillis, locale) {
        if (epochMillis == null) "" else DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale)
            .format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
    }
}
