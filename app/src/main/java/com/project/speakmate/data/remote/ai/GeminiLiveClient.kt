package com.project.speakmate.data.remote.ai

import com.project.speakmate.domain.model.CallFailure
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.utils.AppConstant
import com.project.speakmate.utils.extension.default
import java.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject

class GeminiLiveClient(
    private val httpClient: OkHttpClient,
    private val apiKey: String,
    private val onEvent: (GeminiLiveEvent) -> Unit,
    private val onFailure: (CallFailure) -> Unit
) {
    companion object {
        private const val ENDPOINT =
            "https://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"
        private const val MAX_UPLINK_BACKLOG_BYTES = 16_384L
        private const val MAX_CONSECUTIVE_BACKLOG_DROPS = 5
        private const val MAX_FRAME_BYTES = 524_288
        private const val NORMAL_CLOSURE = 1000
        private const val INPUT_MIME_TYPE = "${GeminiWireField.PCM_MIME_TYPE};rate=${AppConstant.CALL_INPUT_SAMPLE_RATE}"
    }

    private val mutableTurnPending = MutableStateFlow(false)
    val isTurnPending = mutableTurnPending.asStateFlow()
    private val socketLock = Any()
    private val audioParser = GeminiLiveAudioParser()
    private val setupRequest = GeminiLiveSetupRequest()
    private val failureMapper = GeminiLiveFailureMapper()
    private var socket: WebSocket? = null
    private var isOpen = false
    private var isClosed = false
    private var terminalFailure: CallFailure? = null
    private var nextGeneration = 0L
    private var audioGeneration: Long? = null
    private var playbackGeneration: Long? = null
    private var hasSentAudio = false
    private var isModelTurnOpen = false
    private var hasInputDuringModelTurn = false
    private var consecutiveBacklogDrops = 0

    fun connect(scenario: Scenario, resumptionHandle: String? = null) {
        if (apiKey.isBlank()) {
            reportFailure(CallFailure.CONFIGURATION)
            return
        }
        val setup = setupRequest.buildSetup(scenario, resumptionHandle)
        val url = ENDPOINT.toHttpUrl().newBuilder().addQueryParameter(GeminiWireField.API_KEY_PARAMETER, apiKey).build()
        val request = Request.Builder().url(url).build()
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                openSocket(webSocket, setup)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                readMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                if (bytes.size > MAX_FRAME_BYTES) reportFailure(CallFailure.PROTOCOL)
                else readMessage(bytes.utf8())
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(NORMAL_CLOSURE, null)
                reportFailure(failureMapper.getFailure(reason))
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                reportFailure(failureMapper.getFailure(reason))
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                reportFailure(failureMapper.getFailure("", response?.code))
            }
        }
        synchronized(socketLock) {
            if (!isClosed) socket = httpClient.newWebSocket(request, listener)
        }
    }

    fun getConnectionFailure(): CallFailure? = synchronized(socketLock) {
        if (isOpen) null else terminalFailure.default(CallFailure.NETWORK)
    }

    fun enableAudio(): Long? = synchronized(socketLock) {
        if (isOpen && audioGeneration == null) audioGeneration = ++nextGeneration
        audioGeneration
    }

    fun suspendAudio() {
        synchronized(socketLock) {
            audioGeneration = null
            consecutiveBacklogDrops = 0
        }
    }

    fun enableDownlink(generation: Long, requiresIdleTurn: Boolean): CallFailure? = synchronized(socketLock) {
        if (!isOpen) return terminalFailure.default(CallFailure.NETWORK)
        if (requiresIdleTurn && mutableTurnPending.value) return CallFailure.TIMEOUT
        playbackGeneration = generation
        null
    }

    fun suspendMedia() {
        synchronized(socketLock) {
            audioGeneration = null
            playbackGeneration = null
            consecutiveBacklogDrops = 0
        }
    }

    fun sendAudio(generation: Long, audio: ByteArray, hasAudibleInput: Boolean): Boolean {
        val input = JSONObject().put(GeminiWireField.MIME_TYPE, INPUT_MIME_TYPE).put(GeminiWireField.DATA, Base64.getEncoder().encodeToString(audio))
        val message = JSONObject().put(GeminiWireField.REALTIME_INPUT, JSONObject().put(GeminiWireField.AUDIO, input)).toString()
        var shouldFail = false
        val sent = synchronized(socketLock) {
            if (audioGeneration != generation) return false
            val webSocket = socket
            if (!isOpen || webSocket == null) {
                shouldFail = true
                false
            } else if (webSocket.queueSize() + message.length > MAX_UPLINK_BACKLOG_BYTES) {
                consecutiveBacklogDrops++
                if (consecutiveBacklogDrops >= MAX_CONSECUTIVE_BACKLOG_DROPS) shouldFail = true
                false
            } else if (webSocket.send(message)) {
                consecutiveBacklogDrops = 0
                hasSentAudio = true
                if (hasAudibleInput) markInputPending()
                true
            } else {
                shouldFail = true
                false
            }
        }
        if (shouldFail) reportFailure(CallFailure.NETWORK)
        return sent
    }

    fun finishAudioInput() {
        val sent = synchronized(socketLock) {
            if (!hasSentAudio || !isOpen) return
            hasSentAudio = false
            socket?.send(
                JSONObject()
                    .put(GeminiWireField.REALTIME_INPUT, JSONObject().put(GeminiWireField.AUDIO_STREAM_END, true))
                    .toString()
            ) == true
        }
        if (!sent) reportFailure(CallFailure.NETWORK)
    }

    fun close() {
        val webSocket = synchronized(socketLock) { detachSocket() }
        webSocket?.cancel()
    }

    private fun openSocket(webSocket: WebSocket, setup: String) {
        val sent = synchronized(socketLock) {
            if (isClosed || socket !== webSocket) null
            else {
                isOpen = true
                webSocket.send(setup)
            }
        }
        when (sent) {
            null -> webSocket.cancel()
            false -> reportFailure(CallFailure.NETWORK)
            true -> Unit
        }
    }

    private fun readMessage(message: String) {
        val generation = synchronized(socketLock) {
            if (!isOpen) return
            playbackGeneration
        }
        if (message.length > MAX_FRAME_BYTES) {
            reportFailure(CallFailure.PROTOCOL)
            return
        }
        try {
            val frame = JSONObject(message)
            frame.optJSONObject(GeminiWireField.ERROR)?.let { error ->
                reportFailure(failureMapper.getFailure(error.optString(GeminiWireField.STATUS) + " " + error.optString(GeminiWireField.MESSAGE), error.optInt(GeminiWireField.CODE)))
                return
            }
            audioParser.readServerEvents(frame, generation).forEach(::emitEvent)
        } catch (_: Exception) {
            reportFailure(CallFailure.PROTOCOL)
        }
    }

    private fun emitEvent(event: GeminiLiveEvent) {
        synchronized(socketLock) {
            if (!isOpen) return
            when (event) {
                is GeminiLiveEvent.SpeechActivity -> markInputPending()
                is GeminiLiveEvent.Content -> when {
                    event.isTurnFinished || event.isWaitingForInput -> {
                        mutableTurnPending.value = hasInputDuringModelTurn
                        isModelTurnOpen = false
                        hasInputDuringModelTurn = false
                    }
                    event.hasModelTurn -> {
                        isModelTurnOpen = true
                        mutableTurnPending.value = true
                    }
                    else -> Unit
                }
                else -> Unit
            }
        }
        onEvent(event)
    }

    private fun markInputPending() {
        mutableTurnPending.value = true
        if (isModelTurnOpen) hasInputDuringModelTurn = true
    }

    private fun reportFailure(failure: CallFailure) {
        val webSocket = synchronized(socketLock) {
            if (isClosed) return
            terminalFailure = failure
            detachSocket()
        }
        try {
            webSocket?.cancel()
        } finally {
            onFailure(failure)
        }
    }

    private fun detachSocket(): WebSocket? {
        isClosed = true
        isOpen = false
        audioGeneration = null
        playbackGeneration = null
        consecutiveBacklogDrops = 0
        mutableTurnPending.value = false
        return socket.also { socket = null }
    }

}
