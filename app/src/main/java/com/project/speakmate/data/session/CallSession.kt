package com.project.speakmate.data.session

import com.project.speakmate.data.remote.ai.GeminiLiveEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel

class CallSession(
    val media: CallSessionMedia,
    val events: Channel<GeminiLiveEvent>,
    val scope: CoroutineScope,
    val callingAtMillis: Long,
    var mediaRevision: Long
) {
    var timeoutJob: Job? = null
    var resumeJob: Job? = null
    var audioGeneration = 0L
}
