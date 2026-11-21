package com.project.speakmate.domain.rules

import com.project.speakmate.domain.model.CallSessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CallTimingRulesTest {
    companion object {
        private const val START_MILLIS = 1_000L
        private const val PAUSE_MILLIS = 2_000L
        private const val END_MILLIS = 5_000L
        private const val NOW_MILLIS = 9_000L
        private const val EPOCH_START_MILLIS = 1_700_000_000_000L
    }

    @Test
    fun durationIsUnknownBeforeTheCallBecomesActive() {
        assertNull(CallTimingRules.getDurationMillis(CallSessionState(), NOW_MILLIS))
    }

    @Test
    fun durationKeepsRunningAcrossReconnectionAndEarlierPauses() {
        val session = CallSessionState(
            startedAtMillis = START_MILLIS,
            pausedAtMillis = PAUSE_MILLIS,
            totalPausedMillis = PAUSE_MILLIS
        )

        assertEquals(NOW_MILLIS - START_MILLIS, CallTimingRules.getDurationMillis(session, NOW_MILLIS))
    }

    @Test
    fun finalDurationUsesTheManagersEndTime() {
        val session = CallSessionState(startedAtMillis = START_MILLIS, endedAtMillis = END_MILLIS)

        assertEquals(END_MILLIS - START_MILLIS, CallTimingRules.getDurationMillis(session, NOW_MILLIS))
    }

    @Test
    fun durationCannotBeNegative() {
        val session = CallSessionState(startedAtMillis = END_MILLIS, endedAtMillis = START_MILLIS)

        assertEquals(0L, CallTimingRules.getDurationMillis(session, NOW_MILLIS))
    }

    @Test
    fun epochEndUsesTheSameIntervalAsTheCallTimer() {
        val session = CallSessionState(
            startedAtMillis = START_MILLIS,
            endedAtMillis = END_MILLIS,
            startedAtEpochMillis = EPOCH_START_MILLIS
        )

        assertEquals(EPOCH_START_MILLIS + END_MILLIS - START_MILLIS, session.endedAtEpochMillis)
        assertNull(session.copy(endedAtMillis = null).endedAtEpochMillis)
        assertNull(session.copy(startedAtEpochMillis = null).endedAtEpochMillis)
    }
}
