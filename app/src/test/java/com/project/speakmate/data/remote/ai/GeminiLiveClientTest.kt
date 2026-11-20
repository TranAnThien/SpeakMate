package com.project.speakmate.data.remote.ai

import com.project.speakmate.domain.model.CallFailure
import com.project.speakmate.domain.model.Scenario
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiLiveClientTest {
    companion object {
        private const val SYNTHETIC_KEY = "synthetic-key"
        private const val BACKLOG_LIMIT_BYTES = 16_384L
        private const val FRAME_LIMIT_BYTES = 524_288
    }

    private val socket = mockk<WebSocket>(relaxed = true)
    private val httpClient = mockk<OkHttpClient>()
    private val listener = slot<WebSocketListener>()
    private val request = slot<Request>()
    private val sent = mutableListOf<String>()
    private val failures = mutableListOf<CallFailure>()
    private val events = mutableListOf<GeminiLiveEvent>()
    private val scenario = Scenario("scenario", "Cafe", "Order a drink", "Act as a barista")
    private val client = GeminiLiveClient(httpClient, SYNTHETIC_KEY, events::add, failures::add)

    init {
        every { httpClient.newWebSocket(capture(request), capture(listener)) } returns socket
        every { socket.queueSize() } returns 0L
        every { socket.send(any<String>()) } answers { sent.add(firstArg()); true }
    }

    @Test
    fun opensSocketWithSetupAndEncodedCredentialWithoutNetworkAccess() {
        open()
        assertEquals(SYNTHETIC_KEY, request.captured.url.queryParameter("key"))
        assertEquals(1, sent.size)
        assertTrue(JSONObject(sent.single()).has("setup"))
        assertNull(client.getConnectionFailure())
    }

    @Test
    fun blankConfigurationNeverCreatesSocket() {
        GeminiLiveClient(httpClient, "", events::add, failures::add).connect(scenario)
        assertEquals(listOf(CallFailure.CONFIGURATION), failures)
        verify(exactly = 0) { httpClient.newWebSocket(any(), any()) }
    }

    @Test
    fun uplinkRejectsOldGenerationAfterMuteAndResume() {
        open()
        val old = checkNotNull(client.enableAudio())
        assertEquals(old, client.enableAudio())
        client.suspendAudio()
        assertFalse(client.sendAudio(old, byteArrayOf(0, 0), true))
        val current = checkNotNull(client.enableAudio())
        assertNotEquals(old, current)
        assertFalse(client.sendAudio(old, byteArrayOf(0, 0), true))
        assertTrue(client.sendAudio(current, byteArrayOf(1, 2), true))
        assertTrue(failures.isEmpty())
        val audio = JSONObject(sent.last()).getJSONObject("realtimeInput").getJSONObject("audio")
        assertEquals("audio/pcm;rate=16000", audio.getString("mimeType"))
        assertEquals("AQI=", audio.getString("data"))
    }

    @Test
    fun downlinkEventsUseTheActiveGenerationAndSuspendClearsIt() {
        open()
        val first = checkNotNull(client.enableAudio())
        assertNull(client.enableDownlink(first, false))
        val frame = "{\"serverContent\":{\"modelTurn\":{\"parts\":[{\"inlineData\":{\"mimeType\":\"audio/pcm;rate=24000\",\"data\":\"AAA=\"}}]}}}"
        listener.captured.onMessage(socket, frame)
        client.suspendMedia()
        listener.captured.onMessage(socket, frame)
        val resumed = checkNotNull(client.enableAudio())
        assertNotEquals(first, resumed)
        assertNull(client.enableDownlink(resumed, false))
        listener.captured.onMessage(socket, frame)
        assertEquals(listOf(first, null, resumed), events.filterIsInstance<GeminiLiveEvent.Content>().map { it.playbackGeneration })
        assertTrue(failures.isEmpty())
    }

    @Test
    fun closingTwiceRejectsLateOpenMessagesFailuresAndAudio() {
        open()
        val generation = checkNotNull(client.enableAudio())
        client.close()
        client.close()
        listener.captured.onOpen(socket, mockk<Response>())
        listener.captured.onMessage(socket, "{\"setupComplete\":{}}")
        listener.captured.onFailure(socket, IOException(), null)
        assertFalse(client.sendAudio(generation, byteArrayOf(0, 0), true))
        assertTrue(events.isEmpty())
        assertTrue(failures.isEmpty())
    }

    @Test
    fun pendingTurnBlocksDownlinkUntilInputHasBeenConsumed() {
        open()
        val generation = checkNotNull(client.enableAudio())
        client.sendAudio(generation, byteArrayOf(0, 0), true)
        assertTrue(client.isTurnPending.value)
        assertEquals(CallFailure.TIMEOUT, client.enableDownlink(generation, true))
        listener.captured.onMessage(socket, "{\"serverContent\":{\"turnComplete\":true}}")
        assertFalse(client.isTurnPending.value)
        assertNull(client.enableDownlink(generation, true))
    }

    @Test
    fun inputDuringAiTurnRemainsPendingAfterThatTurnCompletes() {
        open()
        val generation = checkNotNull(client.enableAudio())
        client.enableDownlink(generation, false)
        listener.captured.onMessage(socket, "{\"serverContent\":{\"modelTurn\":{\"parts\":[]}}}")
        client.sendAudio(generation, byteArrayOf(0, 0), true)
        listener.captured.onMessage(socket, "{\"serverContent\":{\"turnComplete\":true}}")
        assertTrue(client.isTurnPending.value)
    }

    @Test
    fun audioInputEndIsSentOnceAndOnlyAfterMicrophoneAudio() {
        open()
        client.finishAudioInput()
        assertEquals(1, sent.size)
        val generation = checkNotNull(client.enableAudio())
        client.sendAudio(generation, byteArrayOf(0, 0), false)
        assertFalse(client.isTurnPending.value)
        client.finishAudioInput()
        client.finishAudioInput()
        assertEquals(3, sent.size)
        assertTrue(JSONObject(sent.last()).getJSONObject("realtimeInput").getBoolean("audioStreamEnd"))
    }

    @Test
    fun uplinkBacklogClosesTransportAndReportsOnlyOnce() {
        open()
        every { socket.queueSize() } returns BACKLOG_LIMIT_BYTES
        val generation = checkNotNull(client.enableAudio())
        assertFalse(client.sendAudio(generation, byteArrayOf(0, 0), true))
        listener.captured.onFailure(socket, IOException(), null)
        assertEquals(listOf(CallFailure.NETWORK), failures)
        verify(exactly = 1) { socket.cancel() }
    }

    @Test
    fun malformedAndOversizedFramesFailWithoutEmittingPartialEvents() {
        listOf("{", " ".repeat(FRAME_LIMIT_BYTES + 1)).forEach { frame ->
            val received = mutableListOf<GeminiLiveEvent>()
            val failed = mutableListOf<CallFailure>()
            val current = GeminiLiveClient(httpClient, SYNTHETIC_KEY, received::add, failed::add)
            current.connect(scenario)
            listener.captured.onOpen(socket, mockk<Response>())
            listener.captured.onMessage(socket, frame)
            assertEquals(listOf(CallFailure.PROTOCOL), failed)
            assertTrue(received.isEmpty())
        }
    }

    @Test
    fun providerErrorUsesFailureClassificationAndSuppressesLaterCallbacks() {
        open()
        listener.captured.onMessage(socket, "{\"error\":{\"code\":429,\"status\":\"RESOURCE_EXHAUSTED\"}}")
        listener.captured.onFailure(socket, IOException(), null)
        assertEquals(listOf(CallFailure.QUOTA), failures)
        assertEquals(CallFailure.QUOTA, client.getConnectionFailure())
    }

    private fun open() {
        client.connect(scenario)
        listener.captured.onOpen(socket, mockk<Response>())
    }
}
