package com.project.speakmate.data.session

class CallPronunciationHold {
    fun beginHold(current: CallSession) {
        current.isCallAudioHeld = true
    }

    fun releaseHold(current: CallSession) {
        current.isCallAudioHeld = false
    }

    fun cancelPronunciation(current: CallSession) {
        current.pronunciationRevision++
        current.isPronouncing = false
    }
}
