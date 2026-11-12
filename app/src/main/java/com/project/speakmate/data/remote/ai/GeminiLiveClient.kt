package com.project.speakmate.data.remote.ai

import com.project.speakmate.domain.model.CallFailure
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.utils.AppConstant
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
import org.json.JSONArray
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
        private const val MODEL = "models/gemini-3.8-live"
        private const val MAX_UPLINK_BACKLOG_BYTES = 16_384L
        private const val MAX_FRAME_BYTES = 524_288
        private const val NORMAL_CLOSURE = 1000
        private const val ACCESS_DENIED = 403
        private const val UNAUTHORIZED = 401
        private const val NOT_FOUND = 404
        private const val TOO_MANY_REQUESTS = 429
        private const val PCM_MIME_TYPE = "audio/pcm"
        private const val INPUT_MIME_TYPE = "$PCM_MIME_TYPE;rate=${AppConstant.CALL_INPUT_SAMPLE_RATE}"
        private const val API_KEY_PARAMETER = "key"
        private const val AUDIO_MODALITY = "AUDIO"
        private const val SETUP = "setup"
        private const val MODEL_FIELD = "model"
        private const val GENERATION_CONFIG = "generationConfig"
        private const val RESPONSE_MODALITIES = "responseModalities"
        private const val SYSTEM_INSTRUCTION = "systemInstruction"
        private const val REALTIME_INPUT_CONFIG = "realtimeInputConfig"
        private const val INPUT_AUDIO_TRANSCRIPTION = "inputAudioTranscription"
        private const val OUTPUT_AUDIO_TRANSCRIPTION = "outputAudioTranscription"
        private const val SESSION_RESUMPTION = "sessionResumption"
        private const val RESUMPTION_HANDLE = "handle"
        private const val CONTEXT_WINDOW_COMPRESSION = "contextWindowCompression"
        private const val SLIDING_WINDOW = "slidingWindow"
        private const val AUTOMATIC_ACTIVITY_DETECTION = "automaticActivityDetection"
        private const val DISABLED = "disabled"
        private const val REALTIME_INPUT = "realtimeInput"
        private const val AUDIO = "audio"
        private const val AUDIO_STREAM_END = "audioStreamEnd"
        private const val ERROR = "error"
        private const val STATUS = "status"
        private const val MESSAGE = "message"
        private const val CODE = "code"
        private const val PARTS = "parts"
        private const val TEXT = "text"
        private const val MIME_TYPE = "mimeType"
        private const val DATA = "data"
    }

    private val mutableTurnPending = MutableStateFlow(false)
    val isTurnPending = mutableTurnPending.asStateFlow()
    private val socketLock = Any()
    private val audioParser = GeminiLiveAudioParser()
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

    fun connect(scenario: Scenario, resumptionHandle: String? = null) {
        if (apiKey.isBlank()) {
            reportFailure(CallFailure.CONFIGURATION)
            return
        }
        val setup = buildSetup(scenario, resumptionHandle)
        val url = ENDPOINT.toHttpUrl().newBuilder().addQueryParameter(API_KEY_PARAMETER, apiKey).build()
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
                reportFailure(getFailure(reason))
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                reportFailure(getFailure(reason))
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                reportFailure(getFailure("", response?.code))
            }
        }
        synchronized(socketLock) {
            if (!isClosed) socket = httpClient.newWebSocket(request, listener)
        }
    }

    fun getConnectionFailure(): CallFailure? = synchronized(socketLock) {
        if (isOpen) null else terminalFailure ?: CallFailure.NETWORK
    }

    fun enableAudio(): Long? = synchronized(socketLock) {
        if (isOpen && audioGeneration == null) audioGeneration = ++nextGeneration
        audioGeneration
    }

    fun suspendAudio() {
        synchronized(socketLock) { audioGeneration = null }
    }

    fun enableDownlink(generation: Long, requiresIdleTurn: Boolean): CallFailure? = synchronized(socketLock) {
        if (!isOpen) return terminalFailure ?: CallFailure.NETWORK
        if (requiresIdleTurn && mutableTurnPending.value) return CallFailure.TIMEOUT
        playbackGeneration = generation
        null
    }

    fun suspendMedia() {
        synchronized(socketLock) {
            audioGeneration = null
            playbackGeneration = null
        }
    }

    fun sendAudio(generation: Long, audio: ByteArray, hasAudibleInput: Boolean): Boolean {
        val input = JSONObject().put(MIME_TYPE, INPUT_MIME_TYPE).put(DATA, Base64.getEncoder().encodeToString(audio))
        val message = JSONObject().put(REALTIME_INPUT, JSONObject().put(AUDIO, input)).toString()
        val sent = synchronized(socketLock) {
            if (audioGeneration != generation) return false
            trySend(message).also {
                if (it) {
                    hasSentAudio = true
                    if (hasAudibleInput) markInputPending()
                }
            }
        }
        if (!sent) reportFailure(CallFailure.NETWORK)
        return sent
    }

    fun finishAudioInput() {
        val sent = synchronized(socketLock) {
            if (!hasSentAudio || !isOpen) return
            hasSentAudio = false
            trySend(JSONObject().put(REALTIME_INPUT, JSONObject().put(AUDIO_STREAM_END, true)).toString())
        }
        if (!sent) reportFailure(CallFailure.NETWORK)
    }

    fun close() {
        val webSocket = synchronized(socketLock) { detachSocket() }
        webSocket?.cancel()
    }

    private fun buildSetup(scenario: Scenario, resumptionHandle: String?): String {
        val instruction = "${scenario.name}\n" +
            "${scenario.description}\n${scenario.promptTemplate}"
        val generationConfig = JSONObject().put(RESPONSE_MODALITIES, JSONArray().put(AUDIO_MODALITY))
        val systemInstruction = JSONObject().put(PARTS, JSONArray().put(JSONObject().put(TEXT, instruction)))
        val realtimeConfig = JSONObject().put(AUTOMATIC_ACTIVITY_DETECTION, JSONObject().put(DISABLED, false))
        val setup = JSONObject().put(MODEL_FIELD, MODEL)
            .put(GENERATION_CONFIG, generationConfig)
            .put(SYSTEM_INSTRUCTION, systemInstruction)
            .put(REALTIME_INPUT_CONFIG, realtimeConfig)
            .put(INPUT_AUDIO_TRANSCRIPTION, JSONObject())
            .put(OUTPUT_AUDIO_TRANSCRIPTION, JSONObject())
            .put(SESSION_RESUMPTION, JSONObject().apply { resumptionHandle?.let { put(RESUMPTION_HANDLE, it) } })
            .put(CONTEXT_WINDOW_COMPRESSION, JSONObject().put(SLIDING_WINDOW, JSONObject()))
        return JSONObject().put(SETUP, setup).toString()
    }

    private fun openSocket(webSocket: WebSocket, setup: String) {
        val sent = synchronized(socketLock) {
            if (isClosed || socket !== webSocket) null
            else {
                isOpen = true
                trySend(setup)
            }
        }
        when (sent) {
            null -> webSocket.cancel()
            false -> reportFailure(CallFailure.NETWORK)
            true -> Unit
        }
    }

    private fun trySend(message: String): Boolean {
        val webSocket = socket ?: return false
        return isOpen && webSocket.queueSize() + message.length <= MAX_UPLINK_BACKLOG_BYTES &&
            webSocket.send(message)
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
            frame.optJSONObject(ERROR)?.let { error ->
                reportFailure(getFailure(error.optString(STATUS) + " " + error.optString(MESSAGE), error.optInt(CODE)))
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
        mutableTurnPending.value = false
        return socket.also { socket = null }
    }

    private fun getFailure(reason: String, status: Int? = null): CallFailure {
        val detail = reason.lowercase()
        return when {
            status == TOO_MANY_REQUESTS || "resource_exhausted" in detail || "resource has been exhausted" in detail ||
                "quota" in detail -> CallFailure.QUOTA
            status == UNAUTHORIZED || status == ACCESS_DENIED || "api key" in detail ||
                "permission_denied" in detail || "unauthenticated" in detail -> CallFailure.ACCESS
            status == NOT_FOUND || "model" in detail &&
                ("not found" in detail || "not supported" in detail || "not available" in detail) -> CallFailure.MODEL
            else -> CallFailure.NETWORK
        }
    }
}
