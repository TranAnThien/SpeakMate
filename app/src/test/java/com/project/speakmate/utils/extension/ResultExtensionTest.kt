package com.project.speakmate.utils.extension

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class ResultExtensionTest {
    @Test
    fun rethrowsCancellationInsteadOfReturningFailure() {
        val cancellation = CancellationException()
        val thrown = assertThrows(CancellationException::class.java) {
            runTest { suspendRunCatching { throw cancellation } }
        }
        assertSame(cancellation, thrown)
    }

    @Test
    fun returnsOrdinaryExceptionAsFailure() = runTest {
        val failure = IllegalStateException()
        assertSame(failure, suspendRunCatching { throw failure }.exceptionOrNull())
    }
}
