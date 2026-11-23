package com.project.speakmate.domain.rules

import com.project.speakmate.domain.model.PracticeSaveFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeSaveRetryRulesTest {
    @Test
    fun automaticRetriesHaveThreeIncreasingDelaysAndThenStop() {
        assertEquals(listOf(2_000L, 8_000L, 30_000L), (0 until 3).map(PracticeSaveRetryRules::getDelayMillis))
        assertNull(PracticeSaveRetryRules.getDelayMillis(3))
        assertNull(PracticeSaveRetryRules.getDelayMillis(-1))
    }

    @Test
    fun accessConflictAndInvalidDataCannotBeRetried() {
        listOf(PracticeSaveFailure.ACCESS_DENIED, PracticeSaveFailure.CONFLICT, PracticeSaveFailure.INVALID_COMPLETION).forEach {
            assertFalse(PracticeSaveRetryRules.canRetry(it))
            assertFalse(PracticeSaveRetryRules.canRetryManually(it))
        }
    }

    @Test
    fun transientFailuresCanRetryAutomaticallyOrManually() {
        listOf(PracticeSaveFailure.NETWORK, PracticeSaveFailure.UNKNOWN).forEach {
            assertTrue(PracticeSaveRetryRules.canRetry(it))
            assertTrue(PracticeSaveRetryRules.canRetryManually(it))
        }
    }
}
