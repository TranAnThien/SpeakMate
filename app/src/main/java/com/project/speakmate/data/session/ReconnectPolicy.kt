package com.project.speakmate.data.session

class ReconnectPolicy {
    companion object {
        private const val INITIAL_DELAY_MILLIS = 500L
        private const val MAX_DELAY_MILLIS = 4_000L
        private const val MAX_BACKOFF_STEPS = 3
    }

    fun getDelayMillis(attempt: Int): Long =
        (INITIAL_DELAY_MILLIS shl attempt.coerceIn(0, MAX_BACKOFF_STEPS)).coerceAtMost(MAX_DELAY_MILLIS)
}
