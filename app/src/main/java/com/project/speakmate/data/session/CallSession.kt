package com.project.speakmate.data.session

import com.project.speakmate.data.remote.ai.GeminiLiveEvent
import com.project.speakmate.domain.model.Scenario
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel

class CallSession(
    val media: CallSessionMedia,
    val events: Channel<GeminiLiveEvent>,
    val scope: CoroutineScope,
    val callingAtMillis: Long,
    var mediaRevision: Long,
    val scenario: Scenario
) {
    var timeoutJob: Job? = null
    var resumeJob: Job? = null
    var reconnectJob: Job? = null
    var microphoneJob: Job? = null
    var audioGeneration = 0L
    @Volatile
    var connectionGeneration = 0L
    var resumptionHandle: String? = null
    var reconnectAttempt = 0
    var disconnectedAtMillis: Long? = null
    var shouldResumeAfterReconnect = false
    var isFinalizing = false
    var endRequestedAtMillis: Long? = null
    var pronunciationRevision = 0L
    var isPronouncing = false
    var isCallAudioHeld = false
    val speechEvidence = SpeechEvidenceTracker()
    val transcript = LiveTranscriptAssembler()
}
