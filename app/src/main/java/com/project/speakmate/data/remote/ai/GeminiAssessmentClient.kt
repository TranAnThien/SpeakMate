package com.project.speakmate.data.remote.ai

import com.project.speakmate.data.dto.SpeakingEvaluationResponse
import com.project.speakmate.domain.model.AssessmentEvidence
import com.project.speakmate.domain.model.SpeakingEvaluation
import com.project.speakmate.utils.AppConstant
import java.io.IOException
import java.util.Base64
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class GeminiAssessmentClient @Inject constructor(
    httpClient: OkHttpClient,
    @param:Named(AppConstant.GEMINI_API_KEY) private val apiKey: String
) {
    companion object {
        private const val ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/interactions"
        private const val MODEL_ID = "gemini-3.8-flash"
        private const val CALL_TIMEOUT_SECONDS = 60L
        private const val MAX_ATTEMPTS = 2
        private const val RETRY_DELAY_MILLIS = 1_000L
        private const val MAX_INLINE_REQUEST_BYTES = 20_000_000
        private const val TOO_MANY_REQUESTS = 429
        private const val SERVER_ERROR_START = 500
        private const val SERVER_ERROR_END = 599
        private const val API_KEY_HEADER = "x-goog-api-key"
        private const val JSON_MIME_TYPE = "application/json"
        private const val WAV_MIME_TYPE = "audio/wav"
        private const val MODEL = "model"
        private const val STORE = "store"
        private const val INPUT = "input"
        private const val TYPE = "type"
        private const val TEXT = "text"
        private const val AUDIO = "audio"
        private const val MIME_TYPE = "mime_type"
        private const val DATA = "data"
        private const val RESPONSE_FORMAT = "response_format"
        private const val SCHEMA = "schema"
        private const val STATUS = "status"
        private const val COMPLETED = "completed"
        private const val STEPS = "steps"
        private const val MODEL_OUTPUT = "model_output"
        private const val CONTENT = "content"
    }

    private class HttpFailure(val statusCode: Int) : IOException()

    private val client = httpClient.newBuilder().callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false).build()

    suspend fun evaluate(evidence: AssessmentEvidence, wav: ByteArray): SpeakingEvaluation = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank())
        require(wav.size <= MAX_INLINE_REQUEST_BYTES)
        val input = JSONArray().put(JSONObject().put(TYPE, TEXT).put(TEXT, AssessmentPrompt.create(evidence)))
            .put(JSONObject().put(TYPE, AUDIO).put(MIME_TYPE, WAV_MIME_TYPE).put(DATA, Base64.getEncoder().encodeToString(wav)))
        val format = JSONObject().put(TYPE, TEXT).put(MIME_TYPE, JSON_MIME_TYPE)
            .put(SCHEMA, SpeakingEvaluationResponse.createSchema(evidence.requiredActions.map { it.requiredActionId }))
        val body = JSONObject().put(MODEL, MODEL_ID).put(STORE, false).put(INPUT, input).put(RESPONSE_FORMAT, format)
            .toString().toByteArray(Charsets.UTF_8)
        require(body.size <= MAX_INLINE_REQUEST_BYTES)
        val request = Request.Builder().url(ENDPOINT).header(API_KEY_HEADER, apiKey)
            .post(body.toRequestBody(JSON_MIME_TYPE.toMediaType())).build()
        for (attempt in 0 until MAX_ATTEMPTS) {
            currentCoroutineContext().ensureActive()
            try {
                val response = execute(request)
                return@withContext SpeakingEvaluationResponse(readOutput(response)).toDomain()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: IOException) {
                val isTransient = failure !is HttpFailure || failure.statusCode == TOO_MANY_REQUESTS ||
                    failure.statusCode in SERVER_ERROR_START..SERVER_ERROR_END
                if (!isTransient || attempt == MAX_ATTEMPTS - 1) throw failure
                delay(RETRY_DELAY_MILLIS)
            }
        }
        throw IOException()
    }

    private suspend fun execute(request: Request): String = suspendCancellableCoroutine { continuation ->
        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    try {
                        if (!it.isSuccessful) throw HttpFailure(it.code)
                        val body = it.body ?: throw JSONException(CONTENT)
                        val text = body.string()
                        if (continuation.isActive) continuation.resume(text)
                    } catch (failure: Exception) {
                        if (continuation.isActive) continuation.resumeWithException(failure)
                    }
                }
            }
        })
    }

    private fun readOutput(response: String): String {
        val json = JSONObject(response)
        if (json.getString(STATUS) != COMPLETED) throw JSONException(STATUS)
        val steps = json.getJSONArray(STEPS)
        val text = buildString {
            for (index in 0 until steps.length()) {
                val step = steps.getJSONObject(index)
                if (step.getString(TYPE) != MODEL_OUTPUT) continue
                val content = step.getJSONArray(CONTENT)
                for (part in 0 until content.length()) {
                    val item = content.getJSONObject(part)
                    if (item.getString(TYPE) == TEXT) append(item.getString(TEXT))
                }
            }
        }
        if (text.isBlank()) throw JSONException(CONTENT)
        return text
    }
}
