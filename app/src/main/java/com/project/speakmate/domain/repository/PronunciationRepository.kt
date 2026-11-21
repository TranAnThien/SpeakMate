package com.project.speakmate.domain.repository

interface PronunciationRepository {
    fun playText(text: String)
    fun stop()
    fun release()
}
