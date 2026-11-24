package com.project.speakmate.data.session

import com.project.speakmate.data.audio.AssessmentAudioFileProvider
import com.project.speakmate.data.audio.AssessmentAudioRecorder
import com.project.speakmate.data.audio.PlaybackProgress
import com.project.speakmate.domain.model.CallActivityType
import com.project.speakmate.domain.model.CallFailure
import com.project.speakmate.domain.model.CallInterruption
import com.project.speakmate.domain.model.CallOutcome
import com.project.speakmate.domain.model.CallState
import com.project.speakmate.domain.model.PronunciationState
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.model.SenderType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class CallSessionManagerTest {
    companion object {
        private const val SETUP_TIMEOUT_MILLIS = 15_000L
        private const val RESPONSE_TIMEOUT_MILLIS = 20_000L
        private const val PAUSE_TIMEOUT_MILLIS = 30_000L
        private const val RESUME_TIMEOUT_MILLIS = 4_000L
        private const val TEST_INTERVAL_MILLIS = 1_000L
        private const val EPOCH_START_MILLIS = 1_700_000_000_000L
        private const val PRONUNCIATION_TIMEOUT_MILLIS = 10_000L
        private const val MAX_SERVER_EVENTS = 256
        private const val LONG_SPEECH_MILLIS = 1_000L
        private const val SHORT_NOISE_MILLIS = 300L
        private const val FINALIZE_WINDOW_MILLIS = 3_000L
        private const val RECONNECT_WINDOW_MILLIS = 30_000L
        private const val MICROPHONE_WINDOW_MILLIS = 60_000L
        private const val RESUMPTION_HANDLE = "handle-1"
        private const val TEST_AUDIO_BYTES = 32_000
        private val scenario =
            Scenario(
                scenarioId = "cafe",
                name = "Cafe",
                description = "Order a drink",
                promptTemplate = "Act as a barista"
            )
    }

    @get:Rule val folder = TemporaryFolder()

    @Test
    fun epochEndIsFixedBeforeWaitingForTheFinalTranscript() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        speak(media, LONG_SPEECH_MILLIS)
        media.emitInputTranscription("A large")
        runCurrent()
        val endedAtEpochMillis = EPOCH_START_MILLIS + testScheduler.currentTime

        manager.endCall()
        runCurrent()
        assertEquals(endedAtEpochMillis, manager.sessionState.value.endedAtEpochMillis)
        advanceTimeBy(TEST_INTERVAL_MILLIS)
        media.emitInputTranscription(text = "A large latte", isFinished = true)
        runCurrent()

        assertEquals(CallOutcome.COMPLETED, manager.sessionState.value.outcome)
        assertEquals(endedAtEpochMillis, manager.sessionState.value.endedAtEpochMillis)
    }

    @Test
    fun epochStartSurvivesReconnection() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        val startedAtEpochMillis = manager.sessionState.value.startedAtEpochMillis
        media.emitInputTranscription(text = "Hello", isFinished = true)
        media.emitResumptionHandle(RESUMPTION_HANDLE)
        runCurrent()
        advanceTimeBy(TEST_INTERVAL_MILLIS)
        media.disconnect(CallFailure.NETWORK)
        runCurrent()
        advanceTimeBy(TEST_INTERVAL_MILLIS)
        runCurrent()
        media.acknowledgeSetup()
        runCurrent()

        assertEquals(CallState.ACTIVE, manager.sessionState.value.state)
        assertEquals(startedAtEpochMillis, manager.sessionState.value.startedAtEpochMillis)
    }

    @Test
    fun recordsOnlyAssessmentActivities() = withManager { manager, attempts ->
        val files = AssessmentAudioFileProvider(folder.root)
        for (activityType in CallActivityType.entries) {
            manager.startCall(scenario, activityType)
            runCurrent()
            val media = attempts.last()
            media.audioFiles = files
            media.acknowledgeSetup()
            runCurrent()
            assertEquals(activityType, media.activityType)
            media.emitInputTranscription(text = "Hello", isFinished = true)
            media.sendMicrophoneChunk(ByteArray(TEST_AUDIO_BYTES))
            endAndSettle(manager)
            val state = manager.sessionState.value
            if (activityType == CallActivityType.FREE_PRACTICE) {
                assertEquals(null, state.assessmentAudioId)
                assertEquals(null, state.assessmentAudioDurationMillis)
            } else {
                assertEquals(TEST_INTERVAL_MILLIS, state.assessmentAudioDurationMillis)
                assertTrue(files.getAudioFile(checkNotNull(state.assessmentAudioId)) != null)
            }
        }
    }

    @Test
    fun mutedAndUnsentChunksAreNotRecorded() = withManager { manager, attempts ->
        val media = startAssessment(manager, attempts)
        assertTrue(media.sendMicrophoneChunk(ByteArray(TEST_AUDIO_BYTES)))
        assertFalse(media.sendMicrophoneChunk(chunk = ByteArray(TEST_AUDIO_BYTES), isAcceptedByProvider = false))
        manager.toggleMute()
        runCurrent()
        assertFalse(media.sendMicrophoneChunk(ByteArray(TEST_AUDIO_BYTES)))
        manager.toggleMute()
        runCurrent()
        assertTrue(media.sendMicrophoneChunk(ByteArray(TEST_AUDIO_BYTES)))
        assertEquals(TEST_INTERVAL_MILLIS * 2, media.recorder?.durationMillis)
    }

    @Test
    fun completedAssessmentRetainsWavAndClosesOnce() = withManager { manager, attempts ->
        val media = startAssessment(manager, attempts)
        media.sendMicrophoneChunk(ByteArray(TEST_AUDIO_BYTES))
        media.emitInputTranscription(text = "Hello", isFinished = true)
        runCurrent()
        endAndSettle(manager)
        endAndSettle(manager)
        assertEquals(CallOutcome.COMPLETED, manager.sessionState.value.outcome)
        assertEquals(1, media.recordingFinishCount)
        assertFalse(checkNotNull(media.recorder).append(ByteArray(TEST_AUDIO_BYTES)))
        assertTrue(checkNotNull(media.audioFiles).getAudioFile(checkNotNull(media.recordingAudioId)) != null)
    }

    @Test
    fun failedAndCancelledAssessmentDeleteWav() = withManager { manager, attempts ->
        val failed = startAssessment(manager, attempts)
        failed.sendMicrophoneChunk(ByteArray(TEST_AUDIO_BYTES))
        failed.disconnect(CallFailure.NETWORK)
        runCurrent()
        assertEquals(CallOutcome.FAILED, manager.sessionState.value.outcome)
        assertEquals(1, failed.recordingFinishCount)
        assertEquals(null, checkNotNull(failed.audioFiles).getAudioFile(checkNotNull(failed.recordingAudioId)))
        val cancelled = startAssessment(manager, attempts)
        cancelled.sendMicrophoneChunk(ByteArray(TEST_AUDIO_BYTES))
        endAndSettle(manager)
        assertEquals(CallOutcome.CANCELLED, manager.sessionState.value.outcome)
        assertEquals(null, checkNotNull(cancelled.audioFiles).getAudioFile(checkNotNull(cancelled.recordingAudioId)))
    }

    @Test
    fun assessmentAppendFailureEndsWithAudioFailureAndDeletesWav() = withManager { manager, attempts ->
        val media = startAssessment(manager, attempts)
        assertTrue(media.sendMicrophoneChunk(ByteArray(TEST_AUDIO_BYTES)))
        media.emitInputTranscription(text = "Hello", isFinished = true)
        runCurrent()
        val audioId = checkNotNull(media.recordingAudioId)
        val files = checkNotNull(media.audioFiles)
        assertTrue(files.getAudioFile(audioId) != null)
        media.shouldFailRecordingAppend = true

        assertFalse(media.sendMicrophoneChunk(ByteArray(TEST_AUDIO_BYTES)))
        runCurrent()

        val state = manager.sessionState.value
        assertEquals(CallState.FAILED, state.state)
        assertEquals(CallOutcome.FAILED, state.outcome)
        assertEquals(CallFailure.AUDIO, state.failure)
        assertEquals(null, state.assessmentAudioId)
        assertEquals(null, state.assessmentAudioDurationMillis)
        assertEquals(null, files.getAudioFile(audioId))
        assertEquals(1, media.recordingFinishCount)
        assertEquals(1, media.closeCount)
        assertFalse(media.isAudioOpen)
        endAndSettle(manager)
        assertEquals(1, media.recordingFinishCount)
    }

    @Test
    fun assessmentAppendFailureStillFailsWhenEndPrecedesFailureCallback() = withManager { manager, attempts ->
        val media = startAssessment(manager, attempts)
        media.sendMicrophoneChunk(ByteArray(TEST_AUDIO_BYTES))
        media.emitInputTranscription(text = "Hello", isFinished = true)
        runCurrent()
        val audioId = checkNotNull(media.recordingAudioId)
        media.shouldFailRecordingAppend = true
        media.shouldSuppressRecordingFailureSignal = true
        assertFalse(media.sendMicrophoneChunk(ByteArray(TEST_AUDIO_BYTES)))

        endAndSettle(manager)

        val state = manager.sessionState.value
        assertEquals(CallState.FAILED, state.state)
        assertEquals(CallOutcome.FAILED, state.outcome)
        assertEquals(CallFailure.AUDIO, state.failure)
        assertEquals(null, state.assessmentAudioId)
        assertEquals(null, state.assessmentAudioDurationMillis)
        assertEquals(null, checkNotNull(media.audioFiles).getAudioFile(audioId))
        assertEquals(1, media.recordingFinishCount)
        assertEquals(1, media.closeCount)
    }

    @Test
    fun closeFailureDeletesAssessmentEvidence() = withManager { manager, attempts ->
        val media = startAssessment(manager, attempts)
        media.sendMicrophoneChunk(ByteArray(TEST_AUDIO_BYTES))
        media.emitInputTranscription(text = "Hello", isFinished = true)
        media.shouldFailClose = true
        runCurrent()
        endAndSettle(manager)
        assertEquals(CallOutcome.FAILED, manager.sessionState.value.outcome)
        assertEquals(null, checkNotNull(media.audioFiles).getAudioFile(checkNotNull(media.recordingAudioId)))
        assertEquals(1, media.recordingFinishCount)
    }

    @Test
    fun retryHasNewFileAndRejectsPreviousCaptureCallback() = withManager { manager, attempts ->
        val first = startAssessment(manager, attempts)
        first.sendMicrophoneChunk(ByteArray(TEST_AUDIO_BYTES))
        val stale = first.captureMicrophoneCallback()
        first.emitInputTranscription(text = "Hello", isFinished = true)
        runCurrent()
        endAndSettle(manager)
        val second = startAssessment(manager, attempts)
        assertFalse(stale(ByteArray(TEST_AUDIO_BYTES)))
        assertTrue(second.sendMicrophoneChunk(ByteArray(TEST_AUDIO_BYTES)))
        assertTrue(first.recordingAudioId != second.recordingAudioId)
        assertEquals(TEST_INTERVAL_MILLIS, second.recorder?.durationMillis)
    }

    @Test
    fun assessmentAudioLimitUsesExistingEndPath() = withManager { manager, attempts ->
        val media = startAssessment(manager, attempts)
        media.emitInputTranscription(text = "Hello", isFinished = true)
        runCurrent()
        val seconds = AssessmentAudioRecorder.MAX_ASSESSMENT_AUDIO_MILLIS / TEST_INTERVAL_MILLIS
        repeat(seconds.toInt()) { media.sendMicrophoneChunk(ByteArray(TEST_AUDIO_BYTES)) }
        runCurrent()
        assertEquals(CallOutcome.COMPLETED, manager.sessionState.value.outcome)
        assertEquals(
            AssessmentAudioRecorder.MAX_ASSESSMENT_AUDIO_MILLIS,
            manager.sessionState.value.assessmentAudioDurationMillis
        )
        assertTrue(manager.sessionState.value.hasReachedAssessmentAudioLimit)
        assertEquals(1, media.recordingFinishCount)
    }

    @Test
    fun newAssessmentResetsAudioLimitFlag() = withManager { manager, attempts ->
        val first = startAssessment(manager, attempts)
        first.captureAudioLimitCallback().invoke()
        runCurrent()
        assertTrue(manager.sessionState.value.hasReachedAssessmentAudioLimit)
        startAssessment(manager, attempts)
        assertFalse(manager.sessionState.value.hasReachedAssessmentAudioLimit)
        assertEquals(CallState.ACTIVE, manager.sessionState.value.state)
    }

    @Test
    fun oldAudioLimitCallbackCannotChangeNewSessionFlagOrEndIt() = withManager { manager, attempts ->
        val first = startAssessment(manager, attempts)
        val oldCallback = first.captureAudioLimitCallback()
        endAndSettle(manager)
        val second = startAssessment(manager, attempts)
        oldCallback()
        runCurrent()
        assertFalse(manager.sessionState.value.hasReachedAssessmentAudioLimit)
        assertEquals(CallState.ACTIVE, manager.sessionState.value.state)
        assertTrue(second.isAudioOpen)
        assertEquals(0, second.closeCount)
    }

    private fun TestScope.startAssessment(
        manager: CallSessionManager,
        attempts: List<FakeCallSessionMedia>
    ): FakeCallSessionMedia {
        manager.startCall(scenario, CallActivityType.INITIAL_PLACEMENT)
        runCurrent()
        val media = attempts.last()
        media.audioFiles = AssessmentAudioFileProvider(folder.root)
        media.acknowledgeSetup()
        runCurrent()
        return media
    }

    @Test
    fun startWaitsForSetupAndKeepsOneSession() = withManager { manager, attempts ->
        manager.startCall(scenario, CallActivityType.FREE_PRACTICE)
        manager.startCall(scenario, CallActivityType.FREE_PRACTICE)
        runCurrent()

        assertEquals(CallState.CALLING, manager.sessionState.value.state)
        assertFalse(attempts.single().isAudioOpen)
        attempts.single().acknowledgeSetup()
        runCurrent()

        assertEquals(CallState.ACTIVE, manager.sessionState.value.state)
        assertTrue(attempts.single().isUplinkEnabled)
    }

    @Test
    fun endDuringSetupReleasesOnceAndIgnoresLateSignals() = withManager { manager, attempts ->
        manager.startCall(scenario, CallActivityType.FREE_PRACTICE)
        runCurrent()
        val media = attempts.single()

        manager.endCall()
        endAndSettle(manager)
        media.acknowledgeSetup()
        media.emitFailure(CallFailure.NETWORK)
        advanceTimeBy(SETUP_TIMEOUT_MILLIS)
        runCurrent()

        assertEquals(CallState.ENDED, manager.sessionState.value.state)
        assertEquals(1, media.closeCount)
        assertFalse(media.isAudioOpen)
    }

    @Test
    fun backgroundDuringAudioOpenNeverActivatesMedia() = withManager { manager, attempts ->
        manager.startCall(scenario, CallActivityType.FREE_PRACTICE)
        runCurrent()
        val media = attempts.single()
        media.onOpenAudio = { manager.setForeground(isForeground = false) }

        media.acknowledgeSetup()
        runCurrent()

        assertEquals(CallState.ENDED, manager.sessionState.value.state)
        assertEquals(0, media.playbackStarts)
        assertFalse(media.isUplinkEnabled)
        assertEquals(1, media.audioCloseCount)
    }

    @Test
    fun endDuringDownlinkSetupPreventsPlaybackAndCapture() = withManager { manager, attempts ->
        manager.startCall(scenario, CallActivityType.FREE_PRACTICE)
        runCurrent()
        val media = attempts.single()
        media.onEnableDownlink = { manager.endCall() }

        media.acknowledgeSetup()
        runCurrent()

        assertEquals(CallState.ENDED, manager.sessionState.value.state)
        assertEquals(0, media.playbackStarts)
        assertFalse(media.isUplinkEnabled)
        assertEquals(1, media.closeCount)
    }

    @Test
    fun endDuringCaptureStartNeverPublishesActive() = withManager { manager, attempts ->
        manager.startCall(scenario, CallActivityType.FREE_PRACTICE)
        runCurrent()
        val media = attempts.single()
        media.onStartCapture = {
            assertEquals(CallState.CALLING, manager.sessionState.value.state)
            manager.endCall()
        }

        media.acknowledgeSetup()
        runCurrent()

        assertEquals(CallState.ENDED, manager.sessionState.value.state)
        assertFalse(media.isUplinkEnabled)
        assertEquals(1, media.audioCloseCount)
    }

    @Test
    fun rapidForegroundReturnKeepsMediaPausedUntilResume() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        manager.setForeground(isForeground = false)
        manager.setForeground(isForeground = true)
        assertFalse(media.isUplinkEnabled)
        runCurrent()

        assertEquals(CallState.PAUSED, manager.sessionState.value.state)
        assertFalse(media.isAudioOpen)
        assertTrue(media.hasConnection)
        manager.resumeCall()
        runCurrent()
        assertEquals(CallState.ACTIVE, manager.sessionState.value.state)
    }

    @Test
    fun providerFailureDuringSetupKeepsOriginalCause() = withManager { manager, attempts ->
        manager.startCall(scenario, CallActivityType.FREE_PRACTICE)
        runCurrent()
        val media = attempts.single()
        media.onEnableDownlink = { media.disconnect(CallFailure.QUOTA) }

        media.acknowledgeSetup()
        runCurrent()

        assertEquals(CallFailure.QUOTA, manager.sessionState.value.failure)
        assertEquals(1, media.closeCount)
    }

    @Test
    fun closeAcknowledgesCleanupEvenWhenMediaCloseFails() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        media.shouldFailClose = true

        manager.close()
        manager.close()
        runCurrent()

        assertTrue(manager.sessionState.value.isClosed)
        assertEquals(CallFailure.AUDIO, manager.sessionState.value.failure)
        assertEquals(1, media.closeCount)
    }

    @Test
    fun responseTimeoutStartsAfterSpeechEndInsteadOfIdleSilence() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        advanceTimeBy(RESPONSE_TIMEOUT_MILLIS)
        runCurrent()
        assertEquals(CallState.ACTIVE, manager.sessionState.value.state)

        media.emitSpeechActivity(isSpeaking = true)
        media.emitSpeechActivity(isSpeaking = false)
        runCurrent()
        assertTrue(manager.sessionState.value.isProcessing)
        advanceTimeBy(RESPONSE_TIMEOUT_MILLIS)
        runCurrent()

        assertEquals(CallFailure.TIMEOUT, manager.sessionState.value.failure)
    }

    @Test
    fun timeoutAndLateFailureCannotTerminateRetry() = withManager { manager, attempts ->
        manager.startCall(scenario, CallActivityType.FREE_PRACTICE)
        runCurrent()
        val old = attempts.single()
        advanceTimeBy(SETUP_TIMEOUT_MILLIS)
        runCurrent()
        assertEquals(CallFailure.TIMEOUT, manager.sessionState.value.failure)

        manager.startCall(scenario, CallActivityType.FREE_PRACTICE)
        runCurrent()
        val current = attempts.last()
        old.emitFailure(CallFailure.NETWORK)
        current.acknowledgeSetup()
        runCurrent()

        assertEquals(CallState.ACTIVE, manager.sessionState.value.state)
        assertEquals(1, old.closeCount)
        assertEquals(0, current.closeCount)
    }

    @Test
    fun partialAudioFailureClosesTransportAndAudio() = withManager { manager, attempts ->
        manager.startCall(scenario, CallActivityType.FREE_PRACTICE)
        runCurrent()
        val media = attempts.single()
        media.shouldFailAudioOpen = true
        media.acknowledgeSetup()
        runCurrent()

        assertEquals(CallFailure.AUDIO, manager.sessionState.value.failure)
        assertEquals(1, media.audioCloseCount)
        assertEquals(1, media.closeCount)
    }

    @Test
    fun deniedPermissionClosesAttemptBeforeConnecting() =
        withManager(hasPermission = false) { manager, attempts ->
            manager.startCall(scenario, CallActivityType.FREE_PRACTICE)
            runCurrent()
            val media = attempts.single()

            assertEquals(CallFailure.MICROPHONE_PERMISSION, manager.sessionState.value.failure)
            assertFalse(media.hasConnection)
            assertEquals(1, media.closeCount)
        }

    @Test
    fun mediaConstructionFailurePublishesFailed() =
        withManager(shouldFailConstruction = true) { manager, attempts ->
            manager.startCall(scenario, CallActivityType.FREE_PRACTICE)
            runCurrent()

            assertEquals(CallState.FAILED, manager.sessionState.value.state)
            assertEquals(CallFailure.CONFIGURATION, manager.sessionState.value.failure)
            assertTrue(attempts.isEmpty())
        }

    @Test
    fun muteBlocksUplinkAndLeavesPlaybackOpen() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        manager.toggleMute()
        assertFalse(media.isUplinkEnabled)
        runCurrent()
        assertTrue(manager.sessionState.value.isMuted)
        media.emitAudio(media.playbackGeneration)
        runCurrent()
        assertEquals(1, media.playbackChunks)

        manager.toggleMute()
        runCurrent()
        assertTrue(media.isUplinkEnabled)
        assertEquals(1, attempts.size)
    }

    @Test
    fun transientAudioInterruptionPausesAndResumesOnSameConnection() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        media.interruptAudio()
        runCurrent()

        assertEquals(CallState.PAUSED, manager.sessionState.value.state)
        assertFalse(media.isAudioOpen)
        assertFalse(media.isUplinkEnabled)
        assertTrue(media.hasConnection)

        manager.resumeCall()
        runCurrent()

        assertEquals(CallState.ACTIVE, manager.sessionState.value.state)
        assertTrue(media.isUplinkEnabled)
        assertEquals(1, attempts.size)
        assertEquals(0, media.closeCount)
    }

    @Test
    fun transientAudioInterruptionExpiresLikeBackgroundPause() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        media.interruptAudio()
        runCurrent()
        advanceTimeBy(PAUSE_TIMEOUT_MILLIS)
        runCurrent()

        assertEquals(CallFailure.PAUSE_EXPIRED, manager.sessionState.value.failure)
        assertEquals(1, media.closeCount)
    }

    @Test
    fun serverEventBacklogFailsAsPlaybackBacklog() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        repeat(MAX_SERVER_EVENTS + 2) { media.emitAudio(media.playbackGeneration) }
        runCurrent()

        assertEquals(CallFailure.PLAYBACK_BACKLOG, manager.sessionState.value.failure)
        assertEquals(1, media.closeCount)
    }

    @Test
    fun resumeWaitsForTurnAndRejectsAudioFromPreviousPipeline() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        val previousGeneration = media.playbackGeneration
        media.isTurnIdle.value = false
        manager.setForeground(isForeground = false)
        runCurrent()
        assertFalse(media.isAudioOpen)
        assertTrue(media.hasConnection)

        manager.setForeground(isForeground = true)
        manager.resumeCall()
        runCurrent()
        assertEquals(CallState.PAUSED, manager.sessionState.value.state)
        assertFalse(media.isAudioOpen)
        media.isTurnIdle.value = true
        runCurrent()
        media.emitAudio(previousGeneration)
        media.emitAudio(media.playbackGeneration)
        runCurrent()

        assertEquals(CallState.ACTIVE, manager.sessionState.value.state)
        assertEquals(1, attempts.size)
        assertEquals(1, media.playbackChunks)
    }

    @Test
    fun resumeWithoutTurnBoundaryTimesOut() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        media.isTurnIdle.value = false
        manager.setForeground(isForeground = false)
        runCurrent()
        manager.setForeground(isForeground = true)
        manager.resumeCall()
        runCurrent()
        advanceTimeBy(RESUME_TIMEOUT_MILLIS)
        runCurrent()

        assertEquals(CallFailure.TIMEOUT, manager.sessionState.value.failure)
        assertEquals(1, media.closeCount)
    }

    @Test
    fun pauseExpiresAndReleasesSocket() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        manager.setForeground(isForeground = false)
        runCurrent()
        advanceTimeBy(PAUSE_TIMEOUT_MILLIS)
        runCurrent()

        assertEquals(CallFailure.PAUSE_EXPIRED, manager.sessionState.value.failure)
        assertEquals(1, media.closeCount)
        assertFalse(media.hasConnection)
    }

    @Test
    fun endWhileResumingPreventsAudioFromReopening() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        media.isTurnIdle.value = false
        manager.setForeground(isForeground = false)
        runCurrent()
        manager.setForeground(isForeground = true)
        manager.resumeCall()
        runCurrent()

        endAndSettle(manager)
        media.isTurnIdle.value = true
        runCurrent()

        assertEquals(CallState.ENDED, manager.sessionState.value.state)
        assertFalse(media.isAudioOpen)
        assertEquals(1, media.closeCount)
    }

    @Test
    fun laterResumeRejectsFramesFromRevokedDownlink() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        manager.setForeground(isForeground = false)
        runCurrent()
        var revokedGeneration: Long? = null
        media.onEnableDownlink = { generation ->
            revokedGeneration = generation
            manager.setForeground(isForeground = false)
        }
        manager.setForeground(isForeground = true)
        manager.resumeCall()
        runCurrent()
        assertEquals(CallState.PAUSED, manager.sessionState.value.state)
        assertFalse(media.isAudioOpen)

        media.onEnableDownlink = {}
        manager.setForeground(isForeground = true)
        manager.resumeCall()
        runCurrent()
        media.emitAudio(checkNotNull(revokedGeneration))
        media.emitAudio(media.playbackGeneration)
        runCurrent()

        assertEquals(CallState.ACTIVE, manager.sessionState.value.state)
        assertEquals(1, media.playbackChunks)
    }

    @Test
    fun durationStartsAfterSetupAndKeepsStartAcrossPause() = withManager { manager, attempts ->
        advanceTimeBy(TEST_INTERVAL_MILLIS)
        startActive(manager, attempts)
        val startedAt = testScheduler.currentTime
        assertEquals(startedAt, manager.sessionState.value.startedAtMillis)

        advanceTimeBy(TEST_INTERVAL_MILLIS)
        manager.setForeground(isForeground = false)
        runCurrent()
        advanceTimeBy(TEST_INTERVAL_MILLIS)
        manager.setForeground(isForeground = true)
        manager.resumeCall()
        runCurrent()
        endAndSettle(manager)

        assertEquals(startedAt, manager.sessionState.value.startedAtMillis)
        assertEquals(startedAt + 2 * TEST_INTERVAL_MILLIS, manager.sessionState.value.endedAtMillis)
        assertEquals(TEST_INTERVAL_MILLIS, manager.sessionState.value.totalPausedMillis)
    }

    @Test
    fun endBeforeActiveIsCancelled() = withManager { manager, attempts ->
        manager.startCall(scenario, CallActivityType.FREE_PRACTICE)
        runCurrent()
        attempts.single().emitInputTranscription("Hello")
        endAndSettle(manager)

        assertEquals(CallOutcome.CANCELLED, manager.sessionState.value.outcome)
    }

    @Test
    fun transcriptBeforeActiveIsNotSpeechEvidence() = withManager { manager, attempts ->
        manager.startCall(scenario, CallActivityType.FREE_PRACTICE)
        runCurrent()
        val media = attempts.single()
        media.emitInputTranscription("Hello")
        runCurrent()
        media.acknowledgeSetup()
        runCurrent()
        endAndSettle(manager)

        assertFalse(manager.sessionState.value.hasUserSpeech)
        assertEquals(CallOutcome.CANCELLED, manager.sessionState.value.outcome)
    }

    @Test
    fun endWithoutUserSpeechIsCancelled() = withManager { manager, attempts ->
        startActive(manager, attempts)
        advanceTimeBy(LONG_SPEECH_MILLIS)
        endAndSettle(manager)

        assertEquals(CallState.ENDED, manager.sessionState.value.state)
        assertEquals(CallOutcome.CANCELLED, manager.sessionState.value.outcome)
    }

    @Test
    fun shortNoiseWithoutAnswerIsCancelled() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        speak(media, SHORT_NOISE_MILLIS)
        endAndSettle(manager)

        assertFalse(manager.sessionState.value.hasUserSpeech)
        assertEquals(CallOutcome.CANCELLED, manager.sessionState.value.outcome)
    }

    @Test
    fun sustainedSpeechCompletesSession() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        media.emitSpeechActivity(isSpeaking = true)
        runCurrent()
        assertTrue(manager.sessionState.value.isUserSpeaking)
        advanceTimeBy(LONG_SPEECH_MILLIS)
        media.emitSpeechActivity(isSpeaking = false)
        runCurrent()
        assertTrue(manager.sessionState.value.hasUserSpeech)
        assertFalse(manager.sessionState.value.isUserSpeaking)
        endAndSettle(manager)

        assertEquals(CallOutcome.COMPLETED, manager.sessionState.value.outcome)
    }

    @Test
    fun userTranscriptCompletesSessionWithoutVoiceActivity() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        media.emitInputTranscription("  ")
        runCurrent()
        assertFalse(manager.sessionState.value.hasUserSpeech)
        media.emitInputTranscription("A latte, please")
        runCurrent()
        endAndSettle(manager)

        assertEquals(CallOutcome.COMPLETED, manager.sessionState.value.outcome)
    }

    @Test
    fun modelAnswerAfterShortSpeechCountsAsUserTurn() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        speak(media, SHORT_NOISE_MILLIS)
        media.emitAudio(media.playbackGeneration)
        runCurrent()
        endAndSettle(manager)

        assertEquals(CallOutcome.COMPLETED, manager.sessionState.value.outcome)
    }

    @Test
    fun endDuringOpenSpeechCountsElapsedSpeech() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        media.emitSpeechActivity(isSpeaking = true)
        runCurrent()
        advanceTimeBy(LONG_SPEECH_MILLIS)
        endAndSettle(manager)

        assertEquals(CallOutcome.COMPLETED, manager.sessionState.value.outcome)
    }

    @Test
    fun failureAfterSpeechStaysFailed() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        speak(media, LONG_SPEECH_MILLIS)
        media.disconnect(CallFailure.NETWORK)
        runCurrent()
        endAndSettle(manager)

        assertTrue(manager.sessionState.value.hasUserSpeech)
        assertEquals(CallOutcome.FAILED, manager.sessionState.value.outcome)
    }

    @Test
    fun closingAfterSpeechIsCancelled() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        speak(media, LONG_SPEECH_MILLIS)
        manager.close()
        runCurrent()

        assertEquals(CallOutcome.CANCELLED, manager.sessionState.value.outcome)
    }

    @Test
    fun endWhilePausedAfterSpeechCompletesSession() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        speak(media, LONG_SPEECH_MILLIS)
        media.interruptAudio()
        runCurrent()
        assertEquals(CallState.PAUSED, manager.sessionState.value.state)
        endAndSettle(manager)

        assertEquals(CallOutcome.COMPLETED, manager.sessionState.value.outcome)
    }

    @Test
    fun retryStartsWithoutPreviousSpeechEvidence() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        speak(media, LONG_SPEECH_MILLIS)
        endAndSettle(manager)
        manager.startCall(scenario, CallActivityType.FREE_PRACTICE)
        runCurrent()
        attempts.last().acknowledgeSetup()
        runCurrent()

        assertFalse(manager.sessionState.value.hasUserSpeech)
        assertEquals(null, manager.sessionState.value.outcome)
    }

    @Test
    fun liveTranscriptFollowsTurnsDuringCall() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        media.emitSpeechActivity(isSpeaking = true)
        media.emitInputTranscription("A latte")
        media.emitSpeechActivity(isSpeaking = false)
        media.emitAudio(media.playbackGeneration)
        media.emitOutputTranscription("What size?")
        runCurrent()
        assertFalse(manager.sessionState.value.transcript.last().isFinal)
        media.emitTurnComplete()
        runCurrent()

        val transcript = manager.sessionState.value.transcript
        assertEquals(listOf(SenderType.USER, SenderType.AI), transcript.map { it.senderType })
        assertEquals(listOf("A latte", "What size?"), transcript.map { it.content })
        assertTrue(transcript.last().isFinal)
        assertEquals(CallState.ACTIVE, manager.sessionState.value.state)
    }

    @Test
    fun staleOutputTranscriptionIsIgnored() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        media.emitOutputTranscription("Old answer", generation = (media.playbackGeneration ?: 0L) - 1L)
        runCurrent()

        assertTrue(manager.sessionState.value.transcript.isEmpty())
    }

    @Test
    fun interruptedAnswerIsMarkedInTranscript() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        media.emitAudio(media.playbackGeneration)
        media.emitOutputTranscription("Our special today")
        media.playbackProgress = PlaybackProgress(playedFrames = 50L, enqueuedFrames = 100L)
        media.emitInterrupted()
        runCurrent()

        val message = manager.sessionState.value.transcript.single()
        assertTrue(message.isInterrupted)
        assertEquals("Our", message.content)
        assertEquals(1, media.clearCount)
    }

    @Test
    fun bargeInStopsPlaybackAndShowsUntilUserStopsSpeaking() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        media.emitAudio(media.playbackGeneration)
        media.emitSpeechActivity(isSpeaking = true)
        media.emitInterrupted()
        runCurrent()
        assertTrue(manager.sessionState.value.isAiInterrupted)
        assertFalse(manager.sessionState.value.isPlaying)

        media.emitSpeechActivity(isSpeaking = false)
        runCurrent()

        assertFalse(manager.sessionState.value.isAiInterrupted)
        assertEquals(CallState.ACTIVE, manager.sessionState.value.state)
    }

    @Test
    fun nextModelTurnClearsBargeInAndAcceptsNewAnswer() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        media.emitAudio(media.playbackGeneration)
        media.emitOutputTranscription("Welcome")
        media.playbackProgress = PlaybackProgress(playedFrames = 100L, enqueuedFrames = 100L)
        media.emitInterrupted()
        media.emitOutputTranscription(" to our cafe")
        media.emitAudio(media.playbackGeneration)
        media.emitOutputTranscription("Sure")
        runCurrent()

        assertFalse(manager.sessionState.value.isAiInterrupted)
        assertEquals(listOf("Welcome", "Sure"), manager.sessionState.value.transcript.map { it.content })
    }

    @Test
    fun playbackTurnIsMarkedOncePerAnswer() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        media.emitAudio(media.playbackGeneration)
        media.emitAudio(media.playbackGeneration)
        media.emitTurnComplete()
        media.emitAudio(media.playbackGeneration)
        runCurrent()

        assertEquals(2, media.turnMarks)
    }

    @Test
    fun interruptionWhilePausedIsIgnored() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        media.interruptAudio()
        runCurrent()
        val clearsAfterPause = media.clearCount
        media.emitInterrupted()
        runCurrent()

        assertEquals(clearsAfterPause, media.clearCount)
        assertFalse(manager.sessionState.value.isAiInterrupted)
    }

    @Test
    fun endCallKeepsFinalTranscriptAndMarksOpenAnswer() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        media.emitInputTranscription(text = "Hello", isFinished = true)
        media.emitAudio(media.playbackGeneration)
        media.emitOutputTranscription("Hi, welcome")
        media.playbackProgress = PlaybackProgress(playedFrames = 100L, enqueuedFrames = 100L)
        runCurrent()
        endAndSettle(manager)

        val transcript = manager.sessionState.value.transcript
        assertTrue(transcript.all { it.isFinal })
        assertTrue(transcript.last().isInterrupted)
        assertEquals(CallOutcome.COMPLETED, manager.sessionState.value.outcome)
    }

    @Test
    fun pauseClosesOpenAnswerAndRetryClearsTranscript() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        media.emitAudio(media.playbackGeneration)
        media.emitOutputTranscription("Hello")
        media.playbackProgress = PlaybackProgress(playedFrames = 100L, enqueuedFrames = 100L)
        runCurrent()
        media.interruptAudio()
        runCurrent()
        assertTrue(manager.sessionState.value.transcript.single().isInterrupted)
        endAndSettle(manager)
        manager.startCall(scenario, CallActivityType.FREE_PRACTICE)
        runCurrent()

        assertTrue(manager.sessionState.value.transcript.isEmpty())
    }

    @Test
    fun endCallWaitsForLateFinalUserTranscript() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        speak(media, LONG_SPEECH_MILLIS)
        media.emitInputTranscription("A large")
        runCurrent()
        val endedAt = testScheduler.currentTime
        manager.endCall()
        runCurrent()
        assertTrue(manager.sessionState.value.isFinalizing)
        assertFalse(media.isAudioOpen)
        assertEquals(CallState.ACTIVE, manager.sessionState.value.state)

        advanceTimeBy(TEST_INTERVAL_MILLIS)
        media.emitInputTranscription(text = "A large latte", isFinished = true)
        runCurrent()

        val state = manager.sessionState.value
        assertEquals(CallState.ENDED, state.state)
        assertEquals(CallOutcome.COMPLETED, state.outcome)
        assertEquals(endedAt, state.endedAtMillis)
        assertEquals("A large latte", state.transcript.single().content)
        assertTrue(state.transcript.single().isFinal)
    }

    @Test
    fun finalizeWindowExpiresWithoutChangingEndTime() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        speak(media, LONG_SPEECH_MILLIS)
        val endedAt = testScheduler.currentTime
        manager.endCall()
        runCurrent()
        advanceTimeBy(FINALIZE_WINDOW_MILLIS - 1)
        runCurrent()
        assertTrue(manager.sessionState.value.isFinalizing)
        advanceTimeBy(2)
        runCurrent()

        assertEquals(CallOutcome.COMPLETED, manager.sessionState.value.outcome)
        assertEquals(endedAt, manager.sessionState.value.endedAtMillis)
        assertEquals(1, media.closeCount)
    }

    @Test
    fun socketLossDuringFinalizeStillCompletes() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        speak(media, LONG_SPEECH_MILLIS)
        manager.endCall()
        runCurrent()
        media.disconnect(CallFailure.NETWORK)
        runCurrent()

        assertEquals(CallState.ENDED, manager.sessionState.value.state)
        assertEquals(CallOutcome.COMPLETED, manager.sessionState.value.outcome)
    }

    @Test
    fun repeatedEndDuringFinalizeIsIgnored() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        speak(media, LONG_SPEECH_MILLIS)
        manager.endCall()
        runCurrent()
        manager.endCall()
        manager.setForeground(isForeground = false)
        runCurrent()
        assertTrue(manager.sessionState.value.isFinalizing)
        advanceTimeBy(FINALIZE_WINDOW_MILLIS)
        runCurrent()

        assertEquals(CallOutcome.COMPLETED, manager.sessionState.value.outcome)
        assertEquals(1, media.closeCount)
    }

    @Test
    fun networkLossWithoutResumptionHandleFails() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        media.disconnect(CallFailure.NETWORK)
        runCurrent()

        assertEquals(CallFailure.NETWORK, manager.sessionState.value.failure)
        assertTrue(media.reconnectHandles.isEmpty())
    }

    @Test
    fun networkLossReconnectsAndResumesSameCall() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        val startedAt = manager.sessionState.value.startedAtMillis
        media.emitResumptionHandle(RESUMPTION_HANDLE)
        media.emitInputTranscription(text = "Hello", isFinished = true)
        runCurrent()
        media.disconnect(CallFailure.NETWORK)
        runCurrent()

        val reconnecting = manager.sessionState.value
        assertEquals(CallState.PAUSED, reconnecting.state)
        assertEquals(CallInterruption.NETWORK, reconnecting.interruption)
        assertFalse(media.isAudioOpen)

        advanceTimeBy(RECONNECT_WINDOW_MILLIS / 10)
        runCurrent()
        assertEquals(listOf(RESUMPTION_HANDLE), media.reconnectHandles)
        media.acknowledgeSetup()
        runCurrent()

        val resumed = manager.sessionState.value
        assertEquals(CallState.ACTIVE, resumed.state)
        assertEquals(null, resumed.interruption)
        assertEquals(startedAt, resumed.startedAtMillis)
        assertEquals("Hello", resumed.transcript.single().content)
        assertTrue(media.isAudioOpen)
    }

    @Test
    fun failedReconnectAttemptsRetryUntilWindowExpires() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        media.emitResumptionHandle(RESUMPTION_HANDLE)
        runCurrent()
        media.disconnect(CallFailure.NETWORK)
        runCurrent()
        advanceTimeBy(TEST_INTERVAL_MILLIS)
        runCurrent()
        media.disconnect(CallFailure.NETWORK)
        runCurrent()
        advanceTimeBy(TEST_INTERVAL_MILLIS * 2)
        runCurrent()
        assertTrue(media.reconnectHandles.size >= 2)
        assertEquals(CallInterruption.NETWORK, manager.sessionState.value.interruption)

        advanceTimeBy(RECONNECT_WINDOW_MILLIS)
        runCurrent()

        assertEquals(CallFailure.NETWORK, manager.sessionState.value.failure)
        assertEquals(CallOutcome.FAILED, manager.sessionState.value.outcome)
    }

    @Test
    fun accessFailureDuringReconnectFailsImmediately() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        media.emitResumptionHandle(RESUMPTION_HANDLE)
        runCurrent()
        media.disconnect(CallFailure.NETWORK)
        runCurrent()
        advanceTimeBy(TEST_INTERVAL_MILLIS)
        runCurrent()
        media.disconnect(CallFailure.ACCESS)
        runCurrent()

        assertEquals(CallFailure.ACCESS, manager.sessionState.value.failure)
    }

    @Test
    fun endDuringReconnectUsesCollectedSpeech() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        media.emitResumptionHandle(RESUMPTION_HANDLE)
        speak(media, LONG_SPEECH_MILLIS)
        media.disconnect(CallFailure.NETWORK)
        runCurrent()
        endAndSettle(manager)

        assertEquals(CallState.ENDED, manager.sessionState.value.state)
        assertEquals(CallOutcome.COMPLETED, manager.sessionState.value.outcome)
    }

    @Test
    fun endDuringReconnectWithoutSpeechIsCancelled() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        media.emitResumptionHandle(RESUMPTION_HANDLE)
        runCurrent()
        media.disconnect(CallFailure.NETWORK)
        runCurrent()
        endAndSettle(manager)

        assertEquals(CallOutcome.CANCELLED, manager.sessionState.value.outcome)
    }

    @Test
    fun staleFailureFromPreviousConnectionIsIgnored() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        media.emitResumptionHandle(RESUMPTION_HANDLE)
        runCurrent()
        val staleFailure = media.captureFailureCallback()
        media.disconnect(CallFailure.NETWORK)
        runCurrent()
        advanceTimeBy(TEST_INTERVAL_MILLIS)
        runCurrent()
        media.acknowledgeSetup()
        runCurrent()
        staleFailure(CallFailure.ACCESS)
        runCurrent()

        assertEquals(CallState.ACTIVE, manager.sessionState.value.state)
        assertEquals(null, manager.sessionState.value.failure)
    }

    @Test
    fun silencedMicrophoneBlocksUplinkAndRecovers() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        media.silenceMicrophone(isSilenced = true)
        runCurrent()
        assertEquals(CallInterruption.MICROPHONE, manager.sessionState.value.interruption)
        assertTrue(media.isUplinkBlocked)
        assertEquals(CallState.ACTIVE, manager.sessionState.value.state)

        media.silenceMicrophone(isSilenced = false)
        runCurrent()

        assertEquals(null, manager.sessionState.value.interruption)
        assertFalse(media.isUplinkBlocked)
    }

    @Test
    fun silencedMicrophoneStaysMutedWhenUserHadMuted() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        manager.toggleMute()
        runCurrent()
        media.silenceMicrophone(isSilenced = true)
        runCurrent()
        media.silenceMicrophone(isSilenced = false)
        runCurrent()

        assertTrue(manager.sessionState.value.isMuted)
        assertFalse(media.isUplinkEnabled)
    }

    @Test
    fun silencedMicrophoneFailsAfterWindow() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        media.silenceMicrophone(isSilenced = true)
        runCurrent()
        advanceTimeBy(MICROPHONE_WINDOW_MILLIS)
        runCurrent()

        assertEquals(CallFailure.MICROPHONE_PERMISSION, manager.sessionState.value.failure)
    }

    @Test
    fun muteIsIgnoredWhileMicrophoneIsBlocked() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        media.silenceMicrophone(isSilenced = true)
        runCurrent()
        manager.toggleMute()
        runCurrent()

        assertFalse(manager.sessionState.value.isMuted)
    }

    @Test
    fun pronunciationHoldsCallAudioAndRestoresIt() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        manager.playPronunciation("latte")
        runCurrent()
        assertEquals(listOf("latte"), media.pronouncedTexts)
        assertTrue(media.isUplinkBlocked)
        assertTrue(media.isPlaybackPaused)
        assertEquals(PronunciationState.PLAYING, manager.sessionState.value.pronunciation)

        media.finishPronunciation(isSuccess = true)
        runCurrent()

        assertFalse(media.isUplinkBlocked)
        assertFalse(media.isPlaybackPaused)
        assertTrue(media.isUplinkEnabled)
        assertEquals(PronunciationState.IDLE, manager.sessionState.value.pronunciation)
        assertEquals(CallState.ACTIVE, manager.sessionState.value.state)
    }

    @Test
    fun pronunciationKeepsUserMuteAfterPlayback() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        manager.toggleMute()
        runCurrent()
        manager.playPronunciation("latte")
        runCurrent()
        media.finishPronunciation(isSuccess = true)
        runCurrent()

        assertTrue(manager.sessionState.value.isMuted)
        assertFalse(media.isUplinkEnabled)
        assertFalse(media.isPlaybackPaused)
    }

    @Test
    fun unmuteDuringPronunciationWaitsForPlaybackToFinish() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        manager.toggleMute()
        runCurrent()
        manager.playPronunciation("latte")
        runCurrent()
        manager.toggleMute()
        runCurrent()
        assertFalse(media.isUplinkEnabled)

        media.finishPronunciation(isSuccess = true)
        runCurrent()

        assertFalse(manager.sessionState.value.isMuted)
        assertTrue(media.isUplinkEnabled)
    }

    @Test
    fun pronunciationFailureStaysLocalToSupportTool() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        manager.playPronunciation("latte")
        runCurrent()
        media.finishPronunciation(isSuccess = false)
        runCurrent()

        val state = manager.sessionState.value
        assertEquals(PronunciationState.FAILED, state.pronunciation)
        assertEquals(CallState.ACTIVE, state.state)
        assertEquals(null, state.failure)
        assertFalse(media.isPlaybackPaused)
    }

    @Test
    fun pronunciationEngineErrorDoesNotFailCall() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        media.shouldFailPronunciation = true
        manager.playPronunciation("latte")
        runCurrent()

        assertEquals(PronunciationState.FAILED, manager.sessionState.value.pronunciation)
        assertEquals(CallState.ACTIVE, manager.sessionState.value.state)
        assertFalse(media.isUplinkBlocked)
    }

    @Test
    fun replacedPronunciationIgnoresStaleCompletion() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        manager.playPronunciation("small")
        runCurrent()
        val staleCompletion = media.capturePronunciationCallback()
        manager.playPronunciation("large")
        runCurrent()
        staleCompletion(false)
        runCurrent()

        assertEquals(PronunciationState.PLAYING, manager.sessionState.value.pronunciation)
        assertTrue(media.isPlaybackPaused)
        media.finishPronunciation(isSuccess = true)
        runCurrent()
        assertFalse(media.isPlaybackPaused)
    }

    @Test
    fun stopPronunciationRestoresCallAudio() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        manager.playPronunciation("latte")
        runCurrent()
        manager.stopPronunciation()
        runCurrent()

        assertEquals(1, media.pronunciationStops)
        assertFalse(media.isPlaybackPaused)
        assertTrue(media.isUplinkEnabled)
        assertEquals(PronunciationState.IDLE, manager.sessionState.value.pronunciation)
    }

    @Test
    fun endCallStopsPronunciation() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        manager.playPronunciation("latte")
        runCurrent()
        endAndSettle(manager)

        assertEquals(1, media.pronunciationStops)
        assertEquals(PronunciationState.IDLE, manager.sessionState.value.pronunciation)
    }

    @Test
    fun stopAfterFailedPronunciationClearsError() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        manager.playPronunciation("latte")
        runCurrent()
        media.finishPronunciation(isSuccess = false)
        runCurrent()
        manager.stopPronunciation()
        runCurrent()

        assertEquals(PronunciationState.IDLE, manager.sessionState.value.pronunciation)
        assertEquals(0, media.pronunciationStops)
    }

    @Test
    fun pronunciationIsIgnoredWithoutCall() = withManager { manager, attempts ->
        manager.playPronunciation("latte")
        runCurrent()

        assertTrue(attempts.isEmpty())
        assertEquals(PronunciationState.IDLE, manager.sessionState.value.pronunciation)
    }

    private fun TestScope.endAndSettle(manager: CallSessionManager) {
        manager.endCall()
        runCurrent()
        advanceTimeBy(FINALIZE_WINDOW_MILLIS + 1)
        runCurrent()
    }

    private fun TestScope.speak(media: FakeCallSessionMedia, durationMillis: Long) {
        media.emitSpeechActivity(isSpeaking = true)
        runCurrent()
        advanceTimeBy(durationMillis)
        media.emitSpeechActivity(isSpeaking = false)
        runCurrent()
    }

    @Test
    fun activeMarkerIsWrittenOnceAcrossAReconnectionAndRemovedAfterEnd() {
        val marker = FakeActiveCallMarker()
        withManager(marker = marker) { manager, attempts ->
            val media = startActive(manager, attempts)
            assertEquals(1, marker.saved.size)
            media.emitResumptionHandle(RESUMPTION_HANDLE)
            runCurrent()
            media.disconnect(CallFailure.NETWORK)
            runCurrent()
            advanceTimeBy(TEST_INTERVAL_MILLIS)
            media.acknowledgeSetup()
            runCurrent()
            assertEquals(CallState.ACTIVE, manager.sessionState.value.state)
            assertEquals(1, marker.saved.size)
            manager.endCall()
            runCurrent()
            advanceTimeBy(FINALIZE_WINDOW_MILLIS + 1)
            runCurrent()
            assertTrue(marker.saved.single() in marker.removed)
        }
    }

    @Test
    fun cancellingBeforeActiveNeverWritesAnActiveMarker() {
        val marker = FakeActiveCallMarker()
        withManager(marker = marker) { manager, _ ->
            manager.startCall(scenario, CallActivityType.FREE_PRACTICE)
            runCurrent()
            manager.endCall()
            runCurrent()
            assertTrue(marker.saved.isEmpty())
            assertTrue(marker.removed.isNotEmpty())
        }
    }

    @Test
    fun failedCallsRemoveTheActiveMarker() {
        val marker = FakeActiveCallMarker()
        withManager(marker = marker) { manager, attempts ->
            val media = startActive(manager, attempts)
            media.emitFailure(CallFailure.AUDIO)
            runCurrent()
            assertEquals(CallState.FAILED, manager.sessionState.value.state)
            assertTrue(marker.saved.single() in marker.removed)
        }
    }

    @Test
    fun leavingTheCallClearsItsMarkerEvenWhenCloseIsRepeated() {
        val marker = FakeActiveCallMarker()
        withManager(marker = marker) { manager, attempts ->
            startActive(manager, attempts)
            manager.close()
            manager.close()
            runCurrent()
            assertTrue(marker.saved.single() in marker.removed)
        }
    }

    @Test
    fun markerIoFailureCannotFailOrDelayTheCallState() {
        val marker = FakeActiveCallMarker().also { it.shouldFail = true }
        withManager(marker = marker) { manager, attempts ->
            startActive(manager, attempts)
            assertEquals(CallState.ACTIVE, manager.sessionState.value.state)
            manager.endCall()
            runCurrent()
            advanceTimeBy(FINALIZE_WINDOW_MILLIS + 1)
            runCurrent()
            assertEquals(CallState.ENDED, manager.sessionState.value.state)
        }
    }

    private fun withManager(
        hasPermission: Boolean = true,
        shouldFailConstruction: Boolean = false,
        marker: FakeActiveCallMarker? = null,
        test: TestScope.(CallSessionManager, List<FakeCallSessionMedia>) -> Unit
    ) = runTest {
        val attempts = mutableListOf<FakeCallSessionMedia>()
        val manager =
            CallSessionManager(
                createMedia = {
                    check(!shouldFailConstruction)
                    FakeCallSessionMedia().also {
                        it.hasPermission = hasPermission
                        attempts += it
                    }
                },
                dispatcher = StandardTestDispatcher(testScheduler),
                nowMillis = { testScheduler.currentTime },
                epochMillis = { EPOCH_START_MILLIS + testScheduler.currentTime },
                saveActiveMarker = { marker?.save(it) },
                removeActiveMarker = { marker?.remove(it) }
            )
        manager.setForeground(isForeground = true)
        try {
            test(manager, attempts)
        } finally {
            manager.close()
            runCurrent()
        }
    }

    @Test
    fun pronunciationWaitsForTheActualStartedCallback() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        media.shouldDelayPronunciationStart = true
        manager.playPronunciation("latte")
        runCurrent()
        assertEquals(PronunciationState.LOADING, manager.sessionState.value.pronunciation)
        assertFalse(media.isUplinkEnabled)
        media.startPronunciation()
        runCurrent()
        assertEquals(PronunciationState.PLAYING, manager.sessionState.value.pronunciation)
        advanceTimeBy(PRONUNCIATION_TIMEOUT_MILLIS)
        runCurrent()
        assertEquals(PronunciationState.PLAYING, manager.sessionState.value.pronunciation)
        assertEquals(0, media.pronunciationStops)
        media.finishPronunciation(isSuccess = true)
        runCurrent()
        assertEquals(PronunciationState.IDLE, manager.sessionState.value.pronunciation)
        assertTrue(media.isUplinkEnabled)
        assertEquals(1, attempts.size)
    }

    @Test
    fun pronunciationTimeoutReleasesTheHoldWithoutFailingTheCall() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        media.shouldDelayPronunciationStart = true
        manager.playPronunciation("latte")
        runCurrent()
        advanceTimeBy(PRONUNCIATION_TIMEOUT_MILLIS)
        runCurrent()
        assertEquals(CallState.ACTIVE, manager.sessionState.value.state)
        assertEquals(PronunciationState.FAILED, manager.sessionState.value.pronunciation)
        assertTrue(media.isUplinkEnabled)
        assertTrue(media.pronunciationStops > 0)
        media.startPronunciation()
        media.finishPronunciation(isSuccess = true)
        runCurrent()
        assertEquals(PronunciationState.FAILED, manager.sessionState.value.pronunciation)
        assertEquals(1, attempts.size)
    }

    @Test
    fun endingWhilePronunciationIsLoadingRejectsLateStartedCallbacks() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        media.shouldDelayPronunciationStart = true
        manager.playPronunciation("latte")
        runCurrent()
        manager.endCall()
        runCurrent()
        val ended = manager.sessionState.value
        media.startPronunciation()
        media.finishPronunciation(isSuccess = true)
        advanceTimeBy(PRONUNCIATION_TIMEOUT_MILLIS)
        runCurrent()
        assertEquals(ended, manager.sessionState.value)
        assertEquals(CallState.ENDED, ended.state)
        assertFalse(media.isUplinkEnabled)
    }

    @Test
    fun microphoneRetryKeepsTheOriginalRecoveryDeadlineWhileStillBlocked() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        media.silenceMicrophone(isSilenced = true)
        runCurrent()
        advanceTimeBy(TEST_INTERVAL_MILLIS)
        manager.resumeCall()
        runCurrent()
        assertEquals(CallInterruption.MICROPHONE, manager.sessionState.value.interruption)
        advanceTimeBy(MICROPHONE_WINDOW_MILLIS - TEST_INTERVAL_MILLIS)
        runCurrent()
        assertEquals(CallFailure.MICROPHONE_PERMISSION, manager.sessionState.value.failure)
        assertEquals(1, attempts.size)
    }

    @Test
    fun microphoneRetryUsesTheDeviceSnapshotWithoutOpeningAnotherSession() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        media.silenceMicrophone(isSilenced = true)
        runCurrent()
        val startedAt = manager.sessionState.value.startedAtMillis
        media.isMicrophoneBlocked = false
        manager.resumeCall()
        runCurrent()
        assertEquals(null, manager.sessionState.value.interruption)
        assertEquals(startedAt, manager.sessionState.value.startedAtMillis)
        assertEquals(CallState.ACTIVE, manager.sessionState.value.state)
        assertTrue(media.isUplinkEnabled)
        assertEquals(1, attempts.size)
    }

    @Test
    fun doublePronunciationWhileLoadingStartsOneUtteranceForTheSelectedWord() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        media.shouldDelayPronunciationStart = true
        manager.playPronunciation("latte")
        manager.playPronunciation("latte")
        runCurrent()
        assertEquals(listOf("latte"), media.pronouncedTexts)
        assertEquals("latte", manager.sessionState.value.pronunciationText)
        assertEquals(PronunciationState.LOADING, manager.sessionState.value.pronunciation)
        manager.playPronunciation("large")
        runCurrent()
        assertEquals(listOf("latte", "large"), media.pronouncedTexts)
        assertEquals("large", manager.sessionState.value.pronunciationText)
        assertEquals(1, attempts.size)
    }

    @Test
    fun startCallIgnoredWhileBackgroundedLeavesSessionReadyWithoutOpeningMedia() = withManager { manager, attempts ->
        manager.setForeground(isForeground = false)
        runCurrent()
        manager.startCall(scenario, CallActivityType.FREE_PRACTICE)
        runCurrent()

        assertEquals(CallState.READY, manager.sessionState.value.state)
        assertTrue(attempts.isEmpty())

        manager.setForeground(isForeground = true)
        runCurrent()
        manager.startCall(scenario, CallActivityType.FREE_PRACTICE)
        manager.setForeground(isForeground = false)
        runCurrent()

        assertEquals(CallState.READY, manager.sessionState.value.state)
        assertTrue(attempts.isEmpty())
    }

    @Test
    fun earlyStartFailureDoesNotCarryOverPreviousSessionState() = runTest {
        var nextHasPermission = true
        val attempts = mutableListOf<FakeCallSessionMedia>()
        val manager =
            CallSessionManager(
                createMedia = {
                    FakeCallSessionMedia().also {
                        it.hasPermission = nextHasPermission
                        attempts += it
                    }
                },
                dispatcher = StandardTestDispatcher(testScheduler),
                nowMillis = { testScheduler.currentTime },
                epochMillis = { EPOCH_START_MILLIS + testScheduler.currentTime }
            )
        manager.setForeground(isForeground = true)
        try {
            val first = startAssessment(manager, attempts)
            speak(first, LONG_SPEECH_MILLIS)
            first.emitInputTranscription(text = "Previous turn", isFinished = true)
            first.captureAudioLimitCallback().invoke()
            runCurrent()
            assertTrue(manager.sessionState.value.transcript.isNotEmpty())
            assertTrue(manager.sessionState.value.hasReachedAssessmentAudioLimit)
            assertNotNull(manager.sessionState.value.startedAtEpochMillis)

            nextHasPermission = false
            manager.startCall(scenario, CallActivityType.INITIAL_PLACEMENT)
            runCurrent()

            val failed = manager.sessionState.value
            assertEquals(CallState.FAILED, failed.state)
            assertEquals(CallFailure.MICROPHONE_PERMISSION, failed.failure)
            assertTrue(failed.transcript.isEmpty())
            assertNull(failed.startedAtMillis)
            assertNull(failed.startedAtEpochMillis)
            assertFalse(failed.hasReachedAssessmentAudioLimit)
        } finally {
            manager.close()
            runCurrent()
        }
    }

    private fun TestScope.startActive(
        manager: CallSessionManager,
        attempts: List<FakeCallSessionMedia>
    ): FakeCallSessionMedia {
        manager.startCall(scenario, CallActivityType.FREE_PRACTICE)
        runCurrent()
        val media = attempts.single()
        media.acknowledgeSetup()
        runCurrent()
        return media
    }
}
