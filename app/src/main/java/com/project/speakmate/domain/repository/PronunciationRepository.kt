package com.project.speakmate.domain.repository

import com.project.speakmate.domain.model.PronunciationPlaybackState
import kotlinx.coroutines.flow.StateFlow

interface PronunciationRepository {
    val playback: StateFlow<PronunciationPlaybackState>
    fun playText(text: String)
    fun stop()
    fun release()
}
