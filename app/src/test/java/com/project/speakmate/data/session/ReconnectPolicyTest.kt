package com.project.speakmate.data.session

import org.junit.Assert.assertEquals
import org.junit.Test

class ReconnectPolicyTest {
    private val policy = ReconnectPolicy()

    @Test
    fun delaysGrowThenCap() {
        assertEquals(listOf(500L, 1_000L, 2_000L, 4_000L, 4_000L, 4_000L), (0..5).map(policy::getDelayMillis))
    }

    @Test
    fun negativeAttemptUsesInitialDelay() {
        assertEquals(500L, policy.getDelayMillis(-1))
    }
}
