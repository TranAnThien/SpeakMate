package com.project.speakmate.data.session

import com.project.speakmate.utils.AppConstant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CallPronunciationHold {
    fun play(
        current: CallSession,
        text: String,
        revision: Long,
        sendCommand: (() -> Unit) -> Unit,
        isCurrent: () -> Boolean,
        isLoading: () -> Boolean,
        onStarted: () -> Unit,
        onFinished: (Boolean) -> Unit,
        onTimeout: () -> Unit
    ) {
        current.pronunciationJob =
            current.scope.launch {
                delay(AppConstant.PRONUNCIATION_START_TIMEOUT_MILLIS)
                sendCommand {
                    if (isCurrent() &&
                        current.pronunciationRevision == revision &&
                        isLoading()) {
                        onTimeout()
                    }
                }
            }
        try {
            current.media.speakPronunciation(
                text,
                onStarted = {
                    sendCommand {
                        if (isCurrent() &&
                            current.pronunciationRevision == revision &&
                            current.isPronouncing) {
                            current.pronunciationJob?.cancel()
                            current.pronunciationJob = null
                            onStarted()
                        }
                    }
                }
            ) { isSuccess ->
                sendCommand {
                    if (isCurrent() && current.pronunciationRevision == revision)
                        onFinished(isSuccess)
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            onFinished(false)
        }
    }

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
