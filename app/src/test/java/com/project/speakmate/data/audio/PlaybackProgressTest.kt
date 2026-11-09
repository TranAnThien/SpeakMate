package com.project.speakmate.data.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackProgressTest {
    companion object {
        private const val TOLERANCE = 0.0001f
    }

    @Test
    fun nothingEnqueuedMeansNothingPlayed() {
        assertEquals(0f, PlaybackProgress(10L, 0L).playedRatio, TOLERANCE)
    }

    @Test
    fun ratioFollowsPlayedShare() {
        assertEquals(0.25f, PlaybackProgress(250L, 1_000L).playedRatio, TOLERANCE)
    }

    @Test
    fun ratioIsClampedToRange() {
        assertEquals(1f, PlaybackProgress(1_200L, 1_000L).playedRatio, TOLERANCE)
        assertEquals(0f, PlaybackProgress(-5L, 1_000L).playedRatio, TOLERANCE)
    }
}
