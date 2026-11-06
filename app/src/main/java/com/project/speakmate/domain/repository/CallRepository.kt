package com.project.speakmate.domain.repository

import com.project.speakmate.domain.model.CallSessionState
import com.project.speakmate.domain.model.Scenario
import kotlinx.coroutines.flow.StateFlow

interface CallRepository {
    val sessionState: StateFlow<CallSessionState>
    val audioLevel: StateFlow<Float>

    fun startCall(scenario: Scenario)
    fun toggleMute()
    fun setForeground(isForeground: Boolean)
    fun resumeCall()
    fun endCall()
    fun close()
}
