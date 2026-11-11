package com.project.speakmate.data.remote.ai

import com.project.speakmate.domain.rules.AssessmentTestFixtures
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.After

class GeminiAssessmentClientTest {
    companion object {
        private const val TEST_KEY = "synthetic-key"
        private const val MAX_REQUEST_BYTES = 20_000_000
        private const val WAIT_SECONDS = 5L
    }

    private val httpClients = mutableListOf<OkHttpClient>()

    @After
    fun closeClients() {
        httpClients.forEach { it.dispatcher.executorService.shutdownNow(); it.connectionPool.evictAll() }
    }

    @Test
    fun sendsApprovedModelInlineAudioSchemaAndNoStore() = runBlocking {
        val fake = FakeAssessmentInterceptor()
        val shared = createHttpClient(fake)
        val client = GeminiAssessmentClient(shared, TEST_KEY)
        client.evaluate(AssessmentTestFixtures.getEvidence(), byteArrayOf(0, 0))
        val request = checkNotNull(fake.lastRequest)
        val json = JSONObject(checkNotNull(fake.requestJson))
        assertEquals("/v1beta/interactions", request.url.encodedPath)
        assertEquals(null, request.url.query)
        assertEquals(TEST_KEY, request.header("x-goog-api-key"))
        assertEquals("gemini-3.8-flash", json.getString("model"))
        assertFalse(json.getBoolean("store"))
        assertEquals("audio/wav", json.getJSONArray("input").getJSONObject(1).getString("mime_type"))
        assertEquals("application/json", json.getJSONObject("response_format").getString("mime_type"))
        assertTrue(json.getJSONObject("response_format").has("schema"))
        assertEquals(1, fake.calls.get())
        assertEquals(0, shared.callTimeoutMillis)
        assertTrue(shared.retryOnConnectionFailure)
        assertEquals(TimeUnit.SECONDS.toNanos(60), fake.callTimeoutNanos)
    }

    @Test
    fun temporaryHttpFailuresRetryOnlyOnce() = runBlocking {
        listOf(429, 500, 503).forEach { code ->
            val fake = FakeAssessmentInterceptor().also { it.statusCodes = listOf(code, 200) }
            createClient(fake).evaluate(AssessmentTestFixtures.getEvidence(), byteArrayOf(0, 0))
            assertEquals(2, fake.calls.get())
        }
    }

    @Test
    fun connectionFailureRetriesOnlyOnce() = runBlocking {
        val fake = FakeAssessmentInterceptor().also { it.connectionFailures = 1 }
        createClient(fake).evaluate(AssessmentTestFixtures.getEvidence(), byteArrayOf(0, 0))
        assertEquals(2, fake.calls.get())
    }

    @Test
    fun permanentHttpFailuresNeverRetry() {
        listOf(400, 401, 403, 404, 302).forEach { code ->
            val fake = FakeAssessmentInterceptor().also { it.statusCodes = listOf(code) }
            assertThrows(IOException::class.java) {
                runBlocking { createClient(fake).evaluate(AssessmentTestFixtures.getEvidence(), byteArrayOf(0, 0)) }
            }
            assertEquals(1, fake.calls.get())
        }
    }

    @Test
    fun repeatedTemporaryFailureStopsAtTwoCalls() {
        val fake = FakeAssessmentInterceptor().also { it.statusCodes = listOf(429) }
        assertThrows(IOException::class.java) {
            runBlocking { createClient(fake).evaluate(AssessmentTestFixtures.getEvidence(), byteArrayOf(0, 0)) }
        }
        assertEquals(2, fake.calls.get())
    }

    @Test
    fun invalidJsonAndWrongSchemaDoNotRetry() {
        val wrong = SpeakingEvaluationTestFixtures.getEvaluationJson()
        wrong.getJSONObject("range").remove("isMet")
        listOf("{", SpeakingEvaluationTestFixtures.getInteractionResponse(wrong)).forEach { body ->
            val fake = FakeAssessmentInterceptor().also { it.responseBody = body }
            assertThrows(JSONException::class.java) {
                runBlocking { createClient(fake).evaluate(AssessmentTestFixtures.getEvidence(), byteArrayOf(0, 0)) }
            }
            assertEquals(1, fake.calls.get())
        }
    }

    @Test
    fun oversizedRequestIsRejectedBeforeAnyCall() {
        val fake = FakeAssessmentInterceptor()
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { createClient(fake).evaluate(AssessmentTestFixtures.getEvidence(), ByteArray(MAX_REQUEST_BYTES + 1)) }
        }
        assertEquals(0, fake.calls.get())
    }

    @Test
    fun actualSerializedRequestSizeIsChecked() {
        val fake = FakeAssessmentInterceptor()
        val evidence = AssessmentTestFixtures.getEvidence().copy(communicativeGoal = "é".repeat(MAX_REQUEST_BYTES / 2 + 1))
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { createClient(fake).evaluate(evidence, byteArrayOf(0, 0)) }
        }
        assertEquals(0, fake.calls.get())
    }

    @Test
    fun cancellationCancelsInFlightOkHttpCall() = runBlocking {
        val fake = FakeAssessmentInterceptor().also { it.shouldWaitForCancellation = true }
        val job = launch(Dispatchers.Default) { createClient(fake).evaluate(AssessmentTestFixtures.getEvidence(), byteArrayOf(0, 0)) }
        assertTrue(fake.started.await(WAIT_SECONDS, TimeUnit.SECONDS))
        job.cancelAndJoin()
        assertTrue(fake.cancelled.await(WAIT_SECONDS, TimeUnit.SECONDS))
        assertEquals(1, fake.calls.get())
    }

    private fun createClient(fake: FakeAssessmentInterceptor): GeminiAssessmentClient =
        GeminiAssessmentClient(createHttpClient(fake), TEST_KEY)

    private fun createHttpClient(fake: FakeAssessmentInterceptor): OkHttpClient =
        OkHttpClient.Builder().addInterceptor(fake).build().also { httpClients += it }
}
