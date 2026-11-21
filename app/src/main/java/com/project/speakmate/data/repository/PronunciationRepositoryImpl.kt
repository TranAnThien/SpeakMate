package com.project.speakmate.data.repository

import com.project.speakmate.data.audio.PronunciationPlayer
import com.project.speakmate.domain.repository.PronunciationRepository
import javax.inject.Inject

class PronunciationRepositoryImpl @Inject constructor(
    private val player: PronunciationPlayer
) : PronunciationRepository {
    override fun playText(text: String) {
        player.speak(text) {}
    }

    override fun stop() = player.stop()

    override fun release() = player.release()
}
