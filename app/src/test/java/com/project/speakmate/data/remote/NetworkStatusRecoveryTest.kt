package com.project.speakmate.data.remote

import android.content.Context
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkStatusRecoveryTest {
    companion object {
        private const val RETRY_DELAY_MILLIS = 1_000L
    }

    @Test
    fun collectorFailurePropagatesUnchangedWithoutRetrying() = runTest {
        val provider = NetworkStatusProvider(mockk<Context>(relaxed = true))
        val failure = IllegalArgumentException()
        var attempts = 0
        var caught: Throwable? = null
        val emitted = mutableListOf<Boolean>()

        try {
            provider.observeConnectedWithRecovery {
                flow {
                    attempts++
                    emit(true)
                }
            }.collect {
                emitted.add(it)
                throw failure
            }
        } catch (error: Throwable) {
            caught = error
        }

        assertSame(failure, caught)
        assertEquals(listOf(true), emitted)
        assertEquals(1, attempts)
        assertEquals(0L, testScheduler.currentTime)
    }

    @Test
    fun normalSourceCompletionEmitsDisconnectedAndRetriesAfterTheOriginalDelay() = runTest {
        val provider = NetworkStatusProvider(mockk<Context>(relaxed = true))
        var attempts = 0
        val emitted = mutableListOf<Boolean>()
        val job = launch {
            provider.observeConnectedWithRecovery {
                flow {
                    attempts++
                    emit(true)
                }
            }.collect { emitted.add(it) }
        }
        runCurrent()
        assertEquals(listOf(true, false), emitted)
        assertEquals(1, attempts)

        advanceTimeBy(RETRY_DELAY_MILLIS)
        runCurrent()

        assertEquals(listOf(true, false, true, false), emitted)
        assertEquals(2, attempts)
        job.cancel()
    }
}
