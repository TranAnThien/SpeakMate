package com.project.speakmate.domain.repository

import com.project.speakmate.domain.model.CallSessionState
import com.project.speakmate.domain.model.CallActivityContext
import kotlinx.coroutines.flow.StateFlow

interface CallRepository {
    val sessionState: StateFlow<CallSessionState>
    val audioLevel: StateFlow<Float>

    fun startCall(context: CallActivityContext)
    fun toggleMute()
    fun setForeground(isForeground: Boolean)
    fun resumeCall()
    fun endCall()
    fun playPronunciation(text: String)
    fun stopPronunciation()
    fun close()
}
