package com.project.speakmate.data.repository

import com.project.speakmate.data.session.CallSessionManager
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.repository.CallRepository
import javax.inject.Inject

class CallRepositoryImpl @Inject constructor(private val session: CallSessionManager) : CallRepository {
    override val sessionState get() = session.sessionState
    override val audioLevel get() = session.audioLevel

    override fun startCall(scenario: Scenario) = session.startCall(scenario)
    override fun toggleMute() = session.toggleMute()
    override fun setForeground(isForeground: Boolean) = session.setForeground(isForeground)
    override fun resumeCall() = session.resumeCall()
    override fun endCall() = session.endCall()
    override fun playPronunciation(text: String) = session.playPronunciation(text)
    override fun stopPronunciation() = session.stopPronunciation()
    override fun close() = session.close()
}
