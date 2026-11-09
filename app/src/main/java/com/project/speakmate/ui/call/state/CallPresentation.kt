package com.project.speakmate.ui.call.state

enum class CallPresentation {
    READY,
    CONNECTING,
    LISTENING,
    PROCESSING,
    SPEAKING,
    MUTED,
    PAUSED,
    RECONNECTING,
    MICROPHONE_BLOCKED,
    BARGE_IN,
    AUDIO_UNCLEAR,
    FAILED
}
