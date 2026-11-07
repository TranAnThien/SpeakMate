package com.project.speakmate.domain.model

enum class CallFailure {
    CONFIGURATION,
    ACCESS,
    QUOTA,
    MODEL,
    NETWORK,
    TIMEOUT,
    PROTOCOL,
    MICROPHONE_PERMISSION,
    AUDIO,
    PLAYBACK_BACKLOG,
    PAUSE_EXPIRED
}
