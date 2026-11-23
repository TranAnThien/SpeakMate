package com.project.speakmate.test

import com.project.speakmate.domain.repository.PronunciationRepository

class FakePronunciationRepository : PronunciationRepository {
    override val playback = kotlinx.coroutines.flow.MutableStateFlow(com.project.speakmate.domain.model.PronunciationPlaybackState())
    val playedTexts = mutableListOf<String>()
    var stops = 0
    var releases = 0

    override fun playText(text: String) {
        playedTexts += text
    }

    override fun stop() {
        stops++
    }

    override fun release() {
        releases++
    }
}
