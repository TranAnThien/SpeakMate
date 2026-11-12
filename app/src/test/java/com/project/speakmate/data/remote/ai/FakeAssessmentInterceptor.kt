package com.project.speakmate.data.remote.ai

import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer

class FakeAssessmentInterceptor : Interceptor {
    companion object {
        private const val WAIT_SECONDS = 5L
    }

    val calls = AtomicInteger()
    var statusCodes: List<Int> = listOf(200)
    var responseBody = SpeakingEvaluationTestFixtures.getInteractionResponse()
    var connectionFailures = 0
    var lastRequest: Request? = null
    var requestJson: String? = null
    var shouldWaitForCancellation = false
    var callTimeoutNanos = 0L
    val started = CountDownLatch(1)
    val cancelled = CountDownLatch(1)

    override fun intercept(chain: Interceptor.Chain): Response {
        val index = calls.getAndIncrement()
        lastRequest = chain.request()
        callTimeoutNanos = chain.call().timeout().timeoutNanos()
        val buffer = Buffer()
        chain.request().body?.writeTo(buffer)
        requestJson = buffer.readUtf8()
        started.countDown()
        if (shouldWaitForCancellation) {
            val deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS)
            while (!chain.call().isCanceled() && System.nanoTime() < deadlineNanos) Thread.yield()
            if (chain.call().isCanceled()) cancelled.countDown()
            throw IOException()
        }
        if (index < connectionFailures) throw IOException()
        val statusCode = statusCodes[index.coerceAtMost(statusCodes.lastIndex)]
        return Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(statusCode).message("")
            .header("Location", "https://redirect.invalid/")
            .body(responseBody.toResponseBody("application/json".toMediaType())).build()
    }
}
