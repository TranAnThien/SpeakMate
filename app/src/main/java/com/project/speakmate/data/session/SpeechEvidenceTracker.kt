package com.project.speakmate.data.session

class SpeechEvidenceTracker {
    companion object {
        private const val MIN_USER_SPEECH_MILLIS = 600L
    }

    private var speechStartedAtMillis: Long? = null
    private var totalSpeechMillis = 0L
    private var hasSpeechActivity = false
    private var hasTranscript = false
    private var hasAnsweredTurn = false

    val isUserSpeaking: Boolean get() = speechStartedAtMillis != null

    val hasUserSpeech: Boolean
        get() = hasTranscript || hasAnsweredTurn || totalSpeechMillis >= MIN_USER_SPEECH_MILLIS

    fun startSpeech(nowMillis: Long) {
        if (speechStartedAtMillis != null) return
        speechStartedAtMillis = nowMillis
        hasSpeechActivity = true
    }

    fun endSpeech(nowMillis: Long) {
        val startedAt = speechStartedAtMillis ?: return
        speechStartedAtMillis = null
        totalSpeechMillis += (nowMillis - startedAt).coerceAtLeast(0L)
    }

    fun recordUserTranscript(text: String) {
        if (text.isNotBlank()) hasTranscript = true
    }

    fun recordModelTurn() {
        if (hasSpeechActivity) hasAnsweredTurn = true
    }
}
