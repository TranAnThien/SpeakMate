package com.project.speakmate.data.remote

import android.content.Context
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkStatusProviderTest {
    @Test
    fun recoveryRetriesIndefinitelyWithLongDelayAndResetsAfterEmission() = runTest {
        val provider = NetworkStatusProvider(mockk<Context>(relaxed = true))
        var attempts = 0
        val emitted = mutableListOf<Boolean>()

        val job =
            launch {
                provider
                    .observeConnectedWithRecovery {
                        flow {
                            attempts++
                            if (attempts == 7 || attempts == 9) {
                                emit(true)
                            }
                            throw IllegalStateException("network callback failed")
                        }
                    }
                    .collect { emitted.add(it) }
            }

        runCurrent()
        assertEquals(1, attempts)
        advanceTimeBy(1_000L)
        runCurrent()
        assertEquals(2, attempts)
        advanceTimeBy(2_000L)
        runCurrent()
        assertEquals(3, attempts)
        advanceTimeBy(4_000L)
        runCurrent()
        assertEquals(4, attempts)
        advanceTimeBy(8_000L)
        runCurrent()
        assertEquals(5, attempts)
        advanceTimeBy(16_000L)
        runCurrent()
        assertEquals(6, attempts)
        advanceTimeBy(59_999L)
        runCurrent()
        assertEquals(6, attempts)
        advanceTimeBy(1L)
        runCurrent()
        assertEquals(7, attempts)
        assertTrue(emitted.contains(true))
        advanceTimeBy(1_000L)
        runCurrent()
        assertEquals(8, attempts)
        advanceTimeBy(2_000L)
        runCurrent()
        assertEquals(9, attempts)

        job.cancel()
    }

    @Test
    fun recoveryPropagatesCancellationWithoutRetrying() = runTest {
        val provider = NetworkStatusProvider(mockk<Context>(relaxed = true))
        var attempts = 0
        var caughtCancellation = false

        try {
            provider
                .observeConnectedWithRecovery {
                    flow {
                        attempts++
                        throw CancellationException("cancelled")
                    }
                }
                .collect {}
        } catch (_: CancellationException) {
            caughtCancellation = true
        }

        assertTrue(caughtCancellation)
        assertEquals(1, attempts)
    }
}
