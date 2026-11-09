package com.project.speakmate.data.session

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechEvidenceTrackerTest {
    private val tracker = SpeechEvidenceTracker()

    @Test
    fun hasNoEvidenceBeforeAnySignal() {
        assertFalse(tracker.hasUserSpeech)
        assertFalse(tracker.isUserSpeaking)
    }

    @Test
    fun shortNoiseIsNotSpeech() {
        tracker.startSpeech(0L)
        tracker.endSpeech(300L)

        assertFalse(tracker.hasUserSpeech)
    }

    @Test
    fun speechSegmentsAccumulateToThreshold() {
        tracker.startSpeech(0L)
        tracker.endSpeech(300L)
        tracker.startSpeech(1_000L)
        tracker.endSpeech(1_300L)

        assertTrue(tracker.hasUserSpeech)
    }

    @Test
    fun repeatedStartKeepsFirstStart() {
        tracker.startSpeech(0L)
        tracker.startSpeech(500L)
        tracker.endSpeech(700L)

        assertTrue(tracker.hasUserSpeech)
        assertFalse(tracker.isUserSpeaking)
    }

    @Test
    fun endWithoutStartIsIgnored() {
        tracker.endSpeech(5_000L)

        assertFalse(tracker.hasUserSpeech)
    }

    @Test
    fun blankTranscriptIsNotEvidence() {
        tracker.recordUserTranscript(" \n ")

        assertFalse(tracker.hasUserSpeech)
    }

    @Test
    fun userTranscriptIsEvidence() {
        tracker.recordUserTranscript("Hi")

        assertTrue(tracker.hasUserSpeech)
    }

    @Test
    fun modelTurnWithoutSpeechActivityIsNotEvidence() {
        tracker.recordModelTurn()

        assertFalse(tracker.hasUserSpeech)
    }

    @Test
    fun modelTurnAfterSpeechActivityIsEvidence() {
        tracker.startSpeech(0L)
        tracker.endSpeech(100L)
        tracker.recordModelTurn()

        assertTrue(tracker.hasUserSpeech)
    }
}
