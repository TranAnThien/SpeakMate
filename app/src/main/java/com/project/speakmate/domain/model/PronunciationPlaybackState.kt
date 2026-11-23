package com.project.speakmate.domain.model

data class PronunciationPlaybackState(
    val text: String = "",
    val status: PronunciationState = PronunciationState.IDLE
)
