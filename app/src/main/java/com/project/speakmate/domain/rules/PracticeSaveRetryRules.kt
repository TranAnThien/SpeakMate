package com.project.speakmate.domain.rules

import com.project.speakmate.domain.model.PracticeSaveFailure

object PracticeSaveRetryRules {
    private const val FIRST_DELAY_MILLIS = 2_000L
    private const val SECOND_DELAY_MILLIS = 8_000L
    private const val LAST_DELAY_MILLIS = 30_000L

    fun canRetry(reason: PracticeSaveFailure): Boolean = reason == PracticeSaveFailure.NETWORK || reason == PracticeSaveFailure.UNKNOWN

    fun canRetryManually(reason: PracticeSaveFailure): Boolean = canRetry(reason)

    fun getDelayMillis(attempt: Int): Long? = when (attempt) {
        0 -> FIRST_DELAY_MILLIS
        1 -> SECOND_DELAY_MILLIS
        2 -> LAST_DELAY_MILLIS
        else -> null
    }
}
