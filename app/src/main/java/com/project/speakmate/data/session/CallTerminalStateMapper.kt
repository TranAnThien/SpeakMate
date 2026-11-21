package com.project.speakmate.data.session

import com.project.speakmate.domain.model.CallFailure
import com.project.speakmate.domain.model.CallOutcome
import com.project.speakmate.domain.model.CallSessionState

class CallTerminalStateMapper {
    fun getOutcome(failure: CallFailure?, isUserEnd: Boolean, state: CallSessionState, evidence: SpeechEvidenceTracker): CallOutcome = when {
        failure != null -> CallOutcome.FAILED
        isUserEnd && state.startedAtMillis != null && evidence.hasUserSpeech -> CallOutcome.COMPLETED
        else -> CallOutcome.CANCELLED
    }
}
