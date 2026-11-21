package com.project.speakmate.utils

object AppConstant {
    const val DICTIONARY_LOG_TAG = "DictionaryRepository"
    const val GEMINI_API_KEY = "GEMINI_API_KEY"
    const val CALL_INPUT_SAMPLE_RATE = 16_000
    const val CALL_OUTPUT_SAMPLE_RATE = 24_000
    const val PCM16_BYTES_PER_SAMPLE = 2
    const val BITS_PER_BYTE = 8
    const val MILLIS_PER_SECOND = 1_000
    const val SECONDS_PER_MINUTE = 60L
    const val STATE_SUBSCRIPTION_TIMEOUT_MILLIS = 5_000L
    const val TOO_MANY_REQUESTS = 429
    const val HISTORY_FILTER_ALL = "all"
    const val PRACTICE_CATEGORY_ALL = "ALL"
    const val HISTORY_FILTER_LESSONS = "training_unit"
    const val HISTORY_FILTER_FREE_PRACTICE = "free_practice"
    const val DISPLAY_DATE_PATTERN = "dd/MM/yyyy"
    const val DISPLAY_TIME_PATTERN = "HH:mm"
    const val DISPLAY_DATE_TIME_PATTERN = "dd/MM/yyyy HH:mm"
    const val DURATION_FORMAT = "%02d:%02d"
    const val GOAL_ID_ARGUMENT = "goalId"
    const val IS_REPLAY_ARGUMENT = "isReplay"
    const val UNIT_ID_ARGUMENT = "unitId"
    const val ATTEMPT_ID_ARGUMENT = "attemptId"
}
