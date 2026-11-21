package com.project.speakmate.data.session

import com.project.speakmate.data.audio.AssessmentAudioFileProvider
import com.project.speakmate.data.audio.AssessmentAudioRecorder
import com.project.speakmate.domain.model.CallActivityType
import com.project.speakmate.domain.model.CallOutcome
import java.io.IOException
import kotlinx.coroutines.CancellationException

class CallAssessmentRecording(private val assessmentAudioFiles: AssessmentAudioFileProvider) {
    val lock = Any()
    private var recorder: AssessmentAudioRecorder? = null
    private var recordingAudioId: String? = null
    private var shouldRecordAssessment = false
    private var hasRecordingFailure = false
    private var isAudioLimitReported = false
    private var onAudioLimitReached: (() -> Unit)? = null
    var assessmentAudioId: String? = null
        private set
    var assessmentAudioDurationMillis: Long? = null
        private set

    fun configureAssessmentRecording(activityType: CallActivityType, onLimitReached: () -> Unit) {
        shouldRecordAssessment = activityType != CallActivityType.FREE_PRACTICE
        onAudioLimitReached = onLimitReached
    }

    fun finishAssessmentRecording(outcome: CallOutcome) {
        synchronized(lock) {
            onAudioLimitReached = null
            val current = recorder ?: return
            recorder = null
            try {
                if (hasRecordingFailure) throw IOException()
                current.close()
                if (outcome == CallOutcome.COMPLETED && current.durationMillis > 0L) {
                    assessmentAudioId = recordingAudioId
                    assessmentAudioDurationMillis = current.durationMillis
                } else {
                    current.delete()
                }
            } catch (cancelled: CancellationException) {
                try {
                    current.delete()
                } catch (deleteFailure: Exception) {
                    cancelled.addSuppressed(deleteFailure)
                }
                throw cancelled
            } catch (failure: Exception) {
                current.delete()
                throw failure
            }
        }
    }

    fun resumeCapture(): AssessmentAudioRecorder? {
        return synchronized(lock) {
            if (shouldRecordAssessment && recorder == null) {
                val audioId = assessmentAudioFiles.createAudioId()
                recordingAudioId = audioId
                recorder = assessmentAudioFiles.openRecorder(audioId)
            }
            recorder?.also { it.resumeCapture() }
        }
    }

    fun canAppend(recording: AssessmentAudioRecorder?, chunk: ByteArray): Boolean {
        if (recording != null && !recording.canAppend(chunk)) {
            if (recording.isCapturing) reportAudioLimit()
            return false
        }
        return true
    }

    fun append(recording: AssessmentAudioRecorder, chunk: ByteArray) {
        try {
            if (!recording.append(chunk)) throw IOException()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            hasRecordingFailure = true
            throw IOException(failure)
        }
        if (recording.isAtLimit) reportAudioLimit()
    }

    fun pauseCapture() {
        recorder?.pauseCapture()
    }

    private fun reportAudioLimit() {
        if (isAudioLimitReported) return
        isAudioLimitReported = true
        onAudioLimitReached?.invoke()
    }

}
