package com.project.speakmate.data.session

import com.project.speakmate.domain.model.CallFailure
import com.project.speakmate.domain.model.CallState
import com.project.speakmate.domain.model.Scenario
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CallSessionManagerTest {
    companion object {
        private const val SETUP_TIMEOUT_MILLIS = 15_000L
        private const val RESPONSE_TIMEOUT_MILLIS = 20_000L
        private const val PAUSE_TIMEOUT_MILLIS = 30_000L
        private const val RESUME_TIMEOUT_MILLIS = 4_000L
        private const val TEST_INTERVAL_MILLIS = 1_000L
        private const val MAX_SERVER_EVENTS = 32
        private val scenario = Scenario("cafe", "Cafe", "Order a drink", "Act as a barista")
    }

    @Test
    fun startWaitsForSetupAndKeepsOneSession() = withManager { manager, attempts ->
        manager.startCall(scenario)
        manager.startCall(scenario)
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
        manager.startCall(scenario)
        runCurrent()
        val media = attempts.single()

        manager.endCall()
        manager.endCall()
        runCurrent()
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
        manager.startCall(scenario)
        runCurrent()
        val media = attempts.single()
        media.onOpenAudio = { manager.setForeground(false) }

        media.acknowledgeSetup()
        runCurrent()

        assertEquals(CallState.ENDED, manager.sessionState.value.state)
        assertEquals(0, media.playbackStarts)
        assertFalse(media.isUplinkEnabled)
        assertEquals(1, media.audioCloseCount)
    }

    @Test
    fun endDuringDownlinkSetupPreventsPlaybackAndCapture() = withManager { manager, attempts ->
        manager.startCall(scenario)
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
        manager.startCall(scenario)
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
        manager.setForeground(false)
        manager.setForeground(true)
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
        manager.startCall(scenario)
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

        media.emitSpeechActivity(true)
        media.emitSpeechActivity(false)
        runCurrent()
        assertTrue(manager.sessionState.value.isProcessing)
        advanceTimeBy(RESPONSE_TIMEOUT_MILLIS)
        runCurrent()

        assertEquals(CallFailure.TIMEOUT, manager.sessionState.value.failure)
    }

    @Test
    fun timeoutAndLateFailureCannotTerminateRetry() = withManager { manager, attempts ->
        manager.startCall(scenario)
        runCurrent()
        val old = attempts.single()
        advanceTimeBy(SETUP_TIMEOUT_MILLIS)
        runCurrent()
        assertEquals(CallFailure.TIMEOUT, manager.sessionState.value.failure)

        manager.startCall(scenario)
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
        manager.startCall(scenario)
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
    fun deniedPermissionClosesAttemptBeforeConnecting() = withManager(hasPermission = false) { manager, attempts ->
        manager.startCall(scenario)
        runCurrent()
        val media = attempts.single()

        assertEquals(CallFailure.MICROPHONE_PERMISSION, manager.sessionState.value.failure)
        assertFalse(media.hasConnection)
        assertEquals(1, media.closeCount)
    }

    @Test
    fun mediaConstructionFailurePublishesFailed() = withManager(shouldFailConstruction = true) { manager, attempts ->
        manager.startCall(scenario)
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
        manager.setForeground(false)
        runCurrent()
        assertFalse(media.isAudioOpen)
        assertTrue(media.hasConnection)

        manager.setForeground(true)
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
        manager.setForeground(false)
        runCurrent()
        manager.setForeground(true)
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
        manager.setForeground(false)
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
        manager.setForeground(false)
        runCurrent()
        manager.setForeground(true)
        manager.resumeCall()
        runCurrent()

        manager.endCall()
        runCurrent()
        media.isTurnIdle.value = true
        runCurrent()

        assertEquals(CallState.ENDED, manager.sessionState.value.state)
        assertFalse(media.isAudioOpen)
        assertEquals(1, media.closeCount)
    }

    @Test
    fun laterResumeRejectsFramesFromRevokedDownlink() = withManager { manager, attempts ->
        val media = startActive(manager, attempts)
        manager.setForeground(false)
        runCurrent()
        var revokedGeneration: Long? = null
        media.onEnableDownlink = { generation ->
            revokedGeneration = generation
            manager.setForeground(false)
        }
        manager.setForeground(true)
        manager.resumeCall()
        runCurrent()
        assertEquals(CallState.PAUSED, manager.sessionState.value.state)
        assertFalse(media.isAudioOpen)

        media.onEnableDownlink = {}
        manager.setForeground(true)
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
        manager.setForeground(false)
        runCurrent()
        advanceTimeBy(TEST_INTERVAL_MILLIS)
        manager.setForeground(true)
        manager.resumeCall()
        runCurrent()
        manager.endCall()
        runCurrent()

        assertEquals(startedAt, manager.sessionState.value.startedAtMillis)
        assertEquals(startedAt + 2 * TEST_INTERVAL_MILLIS, manager.sessionState.value.endedAtMillis)
        assertEquals(TEST_INTERVAL_MILLIS, manager.sessionState.value.totalPausedMillis)
    }

    private fun withManager(
        hasPermission: Boolean = true,
        shouldFailConstruction: Boolean = false,
        test: TestScope.(CallSessionManager, List<FakeCallSessionMedia>) -> Unit
    ) = runTest {
        val attempts = mutableListOf<FakeCallSessionMedia>()
        val manager = CallSessionManager(
            createMedia = {
                check(!shouldFailConstruction)
                FakeCallSessionMedia().also {
                    it.hasPermission = hasPermission
                    attempts += it
                }
            },
            dispatcher = StandardTestDispatcher(testScheduler),
            nowMillis = { testScheduler.currentTime }
        )
        manager.setForeground(true)
        try {
            test(manager, attempts)
        } finally {
            manager.close()
            runCurrent()
        }
    }

    private fun TestScope.startActive(manager: CallSessionManager, attempts: List<FakeCallSessionMedia>): FakeCallSessionMedia {
        manager.startCall(scenario)
        runCurrent()
        val media = attempts.single()
        media.acknowledgeSetup()
        runCurrent()
        return media
    }
}
