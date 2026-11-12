package com.project.speakmate.ui.call.state

enum class CallVocabularySaveStatus {
    IDLE,
    SAVING,
    SAVED,
    MOVED_TO_TOP,
    SAVED_REPLACING_OLDEST,
    FAILED
}
