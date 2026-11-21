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
        private val MODELS = listOf("gemini-3.8-flash", "gemini-3.5-flash-lite")
        private const val CONNECT_TIMEOUT_SECONDS = 30L
        private const val WRITE_TIMEOUT_SECONDS = 60L
        private const val CALL_TIMEOUT_SECONDS = 120L
        private const val MAX_ATTEMPTS = 2
        private const val RETRY_DELAY_MILLIS = 1_000L
        private const val MAX_INLINE_REQUEST_BYTES = 20_000_000
        private const val SERVER_ERROR_START = 500
        private const val SERVER_ERROR_END = 599
    }

    private class HttpFailure(val statusCode: Int) : IOException()

    private val client = httpClient.newBuilder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false).build()

    suspend fun evaluate(evidence: AssessmentEvidence, wav: ByteArray): SpeakingEvaluation = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank())
        require(wav.size <= MAX_INLINE_REQUEST_BYTES)
        val input = JSONArray().put(JSONObject().put(GeminiWireField.TYPE, GeminiWireField.TEXT).put(GeminiWireField.TEXT, AssessmentPrompt.create(evidence)))
            .put(JSONObject().put(GeminiWireField.TYPE, GeminiWireField.AUDIO).put(GeminiWireField.INTERACTIONS_MIME_TYPE, GeminiWireField.WAV_MIME_TYPE).put(GeminiWireField.DATA, Base64.getEncoder().encodeToString(wav)))
        val format = JSONObject().put(GeminiWireField.TYPE, GeminiWireField.TEXT).put(GeminiWireField.INTERACTIONS_MIME_TYPE, GeminiWireField.JSON_MIME_TYPE)
            .put(GeminiWireField.SCHEMA, SpeakingEvaluationResponse.createSchema(evidence.requiredActions.map { it.requiredActionId }))
        for (attempt in 0 until MAX_ATTEMPTS) {
            currentCoroutineContext().ensureActive()
            val modelId = MODELS.getOrElse(attempt) { MODELS.last() }
            val body = JSONObject().put(GeminiWireField.MODEL, modelId).put(GeminiWireField.STORE, false).put(GeminiWireField.INPUT, input).put(GeminiWireField.RESPONSE_FORMAT, format)
                .toString().toByteArray(Charsets.UTF_8)
            require(body.size <= MAX_INLINE_REQUEST_BYTES)
            val request = Request.Builder().url(ENDPOINT).header(GeminiWireField.API_KEY_HEADER, apiKey)
                .post(body.toRequestBody(GeminiWireField.JSON_MIME_TYPE.toMediaType())).build()
            try {
                val response = execute(request)
                return@withContext SpeakingEvaluationResponse(readOutput(response)).toDomain()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: IOException) {
                val isTransient = failure !is HttpFailure || failure.statusCode == AppConstant.TOO_MANY_REQUESTS ||
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
                        val body = it.body ?: throw JSONException(GeminiWireField.CONTENT)
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
        if (json.getString(GeminiWireField.STATUS) != GeminiWireField.COMPLETED) throw JSONException(GeminiWireField.STATUS)
        val steps = json.getJSONArray(GeminiWireField.STEPS)
        val text = buildString {
            for (index in 0 until steps.length()) {
                val step = steps.getJSONObject(index)
                if (step.getString(GeminiWireField.TYPE) != GeminiWireField.MODEL_OUTPUT) continue
                val content = step.getJSONArray(GeminiWireField.CONTENT)
                for (part in 0 until content.length()) {
                    val item = content.getJSONObject(part)
                    if (item.getString(GeminiWireField.TYPE) == GeminiWireField.TEXT) append(item.getString(GeminiWireField.TEXT))
                }
            }
        }
        if (text.isBlank()) throw JSONException(GeminiWireField.CONTENT)
        return text
    }
}
