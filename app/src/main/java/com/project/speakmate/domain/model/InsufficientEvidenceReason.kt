package com.project.speakmate.domain.model

enum class InsufficientEvidenceReason {
    CALL_FAILED,
    NO_USER_SPEECH,
    TRANSCRIPT_UNAVAILABLE,
    NOT_ENOUGH_SPEECH,
    AUDIO_UNAVAILABLE,
    EVALUATION_FAILED,
    EVALUATION_INVALID
}
