package com.project.speakmate.data.session

import android.content.Context
import com.project.speakmate.data.audio.AssessmentAudioFileProvider
import com.project.speakmate.data.audio.AudioSessionManager
import com.project.speakmate.data.audio.PlaybackProgress
import com.project.speakmate.domain.model.CallActivityType
import com.project.speakmate.domain.model.CallFailure
import com.project.speakmate.domain.model.CallOutcome
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.utils.AppConstant
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.slot
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class CallSessionMediaAdapterTest {
    companion object {
        private const val BYTES_PER_SECOND = AppConstant.CALL_INPUT_SAMPLE_RATE * AppConstant.PCM16_BYTES_PER_SAMPLE
        private const val CHUNKS_PER_SECOND = 10
        private const val SYNTHETIC_KEY = "synthetic-key"
        private const val AUDIO_FIELD = "audio"
    }

    @get:Rule
    val folder = TemporaryFolder()

    private val context = mockk<Context>()
    private val http = mockk<OkHttpClient>()
    private val socket = mockk<WebSocket>(relaxed = true)
    private val audio = mockk<AudioSessionManager>(relaxed = true)
    private val listener = slot<WebSocketListener>()
    private val request = slot<Request>()
    private val capture = slot<(ByteArray, Boolean) -> Boolean>()
    private val failures = mutableListOf<CallFailure>()
    private val scenario = Scenario("scenario", "Cafe", "Order a drink", "Act as a barista")
    private var adapter: CallSessionMediaAdapter? = null

    @After
    fun closeAdapter() {
        adapter?.close()
    }

    @Test
    fun absentTransportAndPlaybackAreSafeToQueryAndCloseRepeatedly() {
        val media = createAdapter()
        assertEquals(CallFailure.NETWORK, media.getConnectionFailure())
        assertEquals(CallFailure.NETWORK, media.enableDownlink(1L, false))
        assertFalse(media.enqueuePlayback(byteArrayOf(0, 0)))
        assertEquals(PlaybackProgress(0L, 0L), media.clearPlayback())
        media.close()
        media.close()
    }

    @Test
    fun reconnectClosesOldSocketAndSendsResumptionToNewSocket() {
        val media = connect()
        val previous = listener.captured
        media.reconnect(scenario, "resume-handle", {}, failures::add)
        verify(exactly = 1) { socket.cancel() }
        var setup = ""
        every { socket.send(any<String>()) } answers { setup = firstArg(); true }
        listener.captured.onOpen(socket, mockk<Response>())
        assertEquals("resume-handle", JSONObject(setup).getJSONObject("setup").getJSONObject("sessionResumption").getString("handle"))
        previous.onFailure(socket, IOException(), null)
        assertTrue(failures.isEmpty())
        assertNull(media.getConnectionFailure())
    }

    @Test
    fun staleUplinkDoesNotCreateRecordingOrStartCapture() = runTest {
        val media = connect()
        media.configureAssessmentRecording(CallActivityType.TRAINING_UNIT) {}
        media.enableUplink(backgroundScope) { false }
        verify(exactly = 0) { audio.startCapture(any(), any(), any()) }
        assertEquals(0, checkNotNull(folder.root.listFiles()).size)
    }

    @Test
    fun onlySuccessfullySentChunksBecomeAssessmentEvidence() = runTest {
        val media = connect()
        media.configureAssessmentRecording(CallActivityType.TRAINING_UNIT) {}
        media.enableUplink(backgroundScope) { true }
        sendOneSecond()
        every { socket.send(any<String>()) } returns false
        assertFalse(capture.captured(ByteArray(BYTES_PER_SECOND / CHUNKS_PER_SECOND), true))
        media.finishAssessmentRecording(CallOutcome.COMPLETED)
        assertEquals(1_000L, media.assessmentAudioDurationMillis)
        assertTrue(checkNotNull(media.assessmentAudioId).isNotBlank())
        assertEquals(listOf(CallFailure.NETWORK), failures)
    }

    @Test
    fun blockedAndStaleCaptureCannotAppendOrSendMicrophoneAudio() = runTest {
        val media = connect()
        var isCurrent = true
        media.configureAssessmentRecording(CallActivityType.GOAL_ASSESSMENT) {}
        media.enableUplink(backgroundScope) { isCurrent }
        isCurrent = false
        assertFalse(capture.captured(ByteArray(BYTES_PER_SECOND), true))
        isCurrent = true
        media.blockUplink()
        assertFalse(capture.captured(ByteArray(BYTES_PER_SECOND), true))
        media.finishAssessmentRecording(CallOutcome.COMPLETED)
        assertNull(media.assessmentAudioId)
        assertTrue(failures.isEmpty())
    }

    @Test
    fun suspendedUplinkCanResumeTheSameAttemptAndRetainBothSentChunks() = runTest {
        val media = connect()
        media.configureAssessmentRecording(CallActivityType.TRAINING_UNIT) {}
        media.enableUplink(backgroundScope) { true }
        sendOneSecond()
        media.suspendUplink()
        assertFalse(capture.captured(ByteArray(BYTES_PER_SECOND), true))
        media.enableUplink(backgroundScope) { true }
        sendOneSecond()
        media.finishAssessmentRecording(CallOutcome.COMPLETED)
        assertEquals(2_000L, media.assessmentAudioDurationMillis)
        verify(exactly = 1) { audio.stopCapture() }
    }

    @Test
    fun awaitingTurnBoundarySuspendsUntilProviderCompletesTheTurn() = runTest {
        val media = connect()
        listener.captured.onMessage(socket, "{\"serverContent\":{\"modelTurn\":{\"parts\":[]}}}")
        val waiting = async { media.awaitTurnBoundary() }
        runCurrent()
        assertFalse(waiting.isCompleted)
        listener.captured.onMessage(socket, "{\"serverContent\":{\"turnComplete\":true}}")
        runCurrent()
        assertTrue(waiting.isCompleted)
        waiting.await()
    }

    @Test
    fun closeReleasesOwnedAudioAndTransportOnceAndRejectsLateCapture() = runTest {
        val media = connect()
        media.configureAssessmentRecording(CallActivityType.TRAINING_UNIT) {}
        media.enableUplink(backgroundScope) { true }
        media.close()
        media.close()
        assertFalse(capture.captured(ByteArray(BYTES_PER_SECOND), true))
        media.finishAssessmentRecording(CallOutcome.CANCELLED)
        verify(exactly = 1) { audio.close() }
        verify(exactly = 1) { socket.cancel() }
        assertNull(media.assessmentAudioId)
        assertTrue(failures.isEmpty())
    }

    @Test
    fun audioCleanupFailureStillClosesTransportAndDetachesOldAudio() {
        val media = connect()
        every { audio.close() } throws IOException("cleanup")
        try {
            media.close()
            throw AssertionError("Expected cleanup failure")
        } catch (_: IOException) {
            media.close()
        }
        verify(exactly = 1) { socket.cancel() }
        verify(exactly = 1) { audio.close() }
    }

    private fun createAdapter(): CallSessionMediaAdapter =
        CallSessionMediaAdapter(context, http, SYNTHETIC_KEY, AssessmentAudioFileProvider(folder.root)).also { adapter = it }

    private fun sendOneSecond() {
        repeat(CHUNKS_PER_SECOND) { assertTrue(capture.captured(ByteArray(BYTES_PER_SECOND / CHUNKS_PER_SECOND), true)) }
    }

    private fun connect(): CallSessionMediaAdapter {
        every { http.newWebSocket(capture(request), capture(listener)) } returns socket
        every { socket.queueSize() } returns 0L
        every { socket.send(any<String>()) } returns true
        every { audio.startCapture(any(), any(), capture(capture)) } just Runs
        val media = createAdapter()
        CallSessionMediaAdapter::class.java.getDeclaredField(AUDIO_FIELD).apply { isAccessible = true }.set(media, audio)
        media.connect(scenario, {}, failures::add)
        listener.captured.onOpen(socket, mockk<Response>())
        return media
    }
}
