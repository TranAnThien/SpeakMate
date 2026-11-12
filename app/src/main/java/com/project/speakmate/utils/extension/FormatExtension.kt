package com.project.speakmate.utils.extension

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

fun String.parseMarkdownBold(): AnnotatedString {

    val formattedString = this.replace(" **", "\n\n**").trim()

    return buildAnnotatedString {
        val boldRegex = "\\*\\*(.*?)\\*\\*".toRegex()
        var currentIndex = 0

        boldRegex.findAll(formattedString).forEach { matchResult ->
            append(formattedString.substring(currentIndex, matchResult.range.first))

            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(matchResult.groupValues[1])
            }

            currentIndex = matchResult.range.last + 1
        }

        if (currentIndex < formattedString.length) {
            append(formattedString.substring(currentIndex))
        }
    }
}