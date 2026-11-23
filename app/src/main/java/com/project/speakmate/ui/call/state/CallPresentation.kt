package com.project.speakmate.ui.call.state



enum class CallPresentation {
    READY,
    CONNECTING,
    LISTENING,
    USER_SPEAKING,
    FINALIZING,
    PROCESSING,
    SPEAKING,
    PAUSED,
    RECONNECTING,
    MICROPHONE_BLOCKED,
    BARGE_IN,
    AUDIO_UNCLEAR,
    FAILED
}
