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

    data class InputTranscription(
        val text: String,
        val isFinished: Boolean,
        val playbackGeneration: Long?
    ) : GeminiLiveEvent

    data class OutputTranscription(
        val text: String,
        val isFinished: Boolean,
        val playbackGeneration: Long?
    ) : GeminiLiveEvent

    data class Interrupted(val playbackGeneration: Long?) : GeminiLiveEvent

    data class GenerationComplete(val playbackGeneration: Long?) : GeminiLiveEvent

    data class ResumptionUpdate(
        val handle: String,
        val isResumable: Boolean
    ) : GeminiLiveEvent

    data class GoAway(val timeLeftMillis: Long?) : GeminiLiveEvent
}
