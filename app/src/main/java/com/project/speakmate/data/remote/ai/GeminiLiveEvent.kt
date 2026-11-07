package com.project.speakmate.data.remote.ai

sealed interface GeminiLiveEvent {
    data object SetupComplete : GeminiLiveEvent
    data class SpeechActivity(
        val isSpeaking: Boolean,
        val playbackGeneration: Long?
    ) : GeminiLiveEvent

    data class Content(
        val audio: List<ByteArray>,
        val playbackGeneration: Long?,
        val hasModelTurn: Boolean,
        val isTurnFinished: Boolean,
        val isWaitingForInput: Boolean
    ) : GeminiLiveEvent
}
