package com.project.speakmate.data.audio

import android.media.AudioAttributes
import android.media.AudioTrack
import android.os.SystemClock
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AudioPlaybackTest {
    companion object {
        private const val QUEUE_CAPACITY = 512
        private const val BYTE_BUDGET = 576_000
        private const val POLL_MILLIS = 15L
        private const val STALL_MILLIS = 2_000L
    }

    @After
    fun releaseClockMock() {
        unmockkStatic(SystemClock::class)
    }

    @Test
    fun byteBudgetAcceptsTheBoundaryAndRejectedBytesDoNotConsumeCapacity() {
        val playback = AudioPlayback()
        assertFalse(playback.enqueue(ByteArray(BYTE_BUDGET + 1)))
        assertTrue(playback.enqueue(ByteArray(BYTE_BUDGET)))
        assertFalse(playback.enqueue(byteArrayOf(0, 0)))
        assertEquals(PlaybackProgress(0L, BYTE_BUDGET / 2L), playback.clear())
        assertTrue(playback.enqueue(ByteArray(BYTE_BUDGET)))
        playback.close()
    }

    @Test
    fun chunkCapacityRejectsOverflowAndClearReleasesAllCapacity() {
        val playback = AudioPlayback()
        repeat(QUEUE_CAPACITY) { assertTrue(playback.enqueue(byteArrayOf(0, 0))) }
        assertFalse(playback.enqueue(byteArrayOf(0, 0)))
        assertEquals(PlaybackProgress(0L, QUEUE_CAPACITY.toLong()), playback.clear())
        repeat(QUEUE_CAPACITY) { assertTrue(playback.enqueue(byteArrayOf(0, 0))) }
        playback.close()
    }

    @Test
    fun turnProgressExcludesAudioEnqueuedBeforeTheCurrentTurn() {
        val playback = AudioPlayback()
        playback.enqueue(byteArrayOf(0, 0, 0, 0))
        playback.markTurn()
        playback.enqueue(byteArrayOf(0, 0))
        assertEquals(PlaybackProgress(0L, 1L), playback.clear())
        assertEquals(PlaybackProgress(0L, 0L), playback.clear())
        playback.close()
    }

    @Test
    fun pauseDoesNotDiscardQueuedAudio() {
        val playback = AudioPlayback()
        playback.enqueue(byteArrayOf(0, 0))
        playback.setPaused(true)
        playback.setPaused(false)
        assertEquals(PlaybackProgress(0L, 1L), playback.clear())
        playback.close()
    }

    @Test
    fun closeIsRepeatSafeAndRejectsLateChunks() {
        val playback = AudioPlayback()
        playback.enqueue(byteArrayOf(0, 0))
        playback.close()
        playback.close()
        repeat(2) { assertFalse(playback.enqueue(byteArrayOf(0, 0))) }
    }

    @Test
    fun cancellationReleasesTrackAndDoesNotWriteWhenSessionIsStale() = runTest {
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } answers { testScheduler.currentTime }
        val track = createTrack()
        val playback = createPlayback(track)
        val isCurrent = AtomicBoolean(true)
        val job = backgroundScope.launch { playback.play(mockk<AudioAttributes>(), isCurrent::get, { 0f }) { _, _ -> } }
        runCurrent()
        job.cancelAndJoin()
        verify(exactly = 1) { track.release() }
        verify(exactly = 1) { track.flush() }
        val staleTrack = createTrack()
        val stale = createPlayback(staleTrack)
        stale.enqueue(byteArrayOf(0, 0))
        stale.play(mockk<AudioAttributes>(), { false }, { 0f }) { _, _ -> }
        verify(exactly = 0) { staleTrack.write(any<ByteArray>(), any(), any(), any()) }
        verify(exactly = 1) { staleTrack.release() }
    }

    @Test
    fun clearingAnInFlightChunkFlushesItBeforeTheNextWrite() = runTest {
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } answers { testScheduler.currentTime }
        val track = createTrack()
        every { track.write(any<ByteArray>(), any(), any(), any()) } returns 0
        val playback = createPlayback(track)
        playback.enqueue(byteArrayOf(0, 0))
        val job = backgroundScope.launch { playback.play(mockk<AudioAttributes>(), { true }, { 0f }) { _, _ -> } }
        runCurrent()
        assertEquals(PlaybackProgress(0L, 1L), playback.clear())
        advanceTimeBy(POLL_MILLIS)
        runCurrent()
        verify(exactly = 1) { track.write(any<ByteArray>(), any(), any(), any()) }
        verify(exactly = 1) { track.flush() }
        job.cancelAndJoin()
        verify(exactly = 1) { track.release() }
    }

    @Test
    fun stalledWritesFailAtTheControlledTimeoutAndReleaseTrack() = runTest {
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } answers { testScheduler.currentTime }
        val track = createTrack()
        every { track.write(any<ByteArray>(), any(), any(), any()) } returns 0
        val playback = createPlayback(track)
        playback.enqueue(byteArrayOf(0, 0))
        var hasFailed = false
        try {
            playback.play(mockk<AudioAttributes>(), { true }, { 0f }) { _, _ -> }
        } catch (_: IllegalStateException) {
            hasFailed = true
        }
        assertTrue(hasFailed)
        assertTrue(testScheduler.currentTime in STALL_MILLIS..(STALL_MILLIS + POLL_MILLIS))
        verify(exactly = 1) { track.release() }
    }

    private fun createTrack(): AudioTrack = mockk<AudioTrack>(relaxed = true).also { track ->
        every { track.state } returns AudioTrack.STATE_INITIALIZED
        every { track.playState } returns AudioTrack.PLAYSTATE_PLAYING
        every { track.playbackHeadPosition } returns 0
    }

    private fun createPlayback(track: AudioTrack): AudioPlayback = spyk(AudioPlayback(), recordPrivateCalls = true).also { playback ->
        every { playback["createTrack"](any<AudioAttributes>()) } returns track
    }
}
