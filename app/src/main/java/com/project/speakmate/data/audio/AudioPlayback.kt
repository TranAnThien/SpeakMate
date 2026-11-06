package com.project.speakmate.data.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.SystemClock
import com.project.speakmate.utils.AppConstant
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class AudioPlayback {
    companion object {
        private const val MAX_QUEUED_CHUNKS = 512
        private const val MAX_BUFFER_SECONDS = 12
        private const val MAX_QUEUED_BYTES =
            AppConstant.CALL_OUTPUT_SAMPLE_RATE * AppConstant.PCM16_BYTES_PER_SAMPLE * MAX_BUFFER_SECONDS
        private const val PLAYBACK_POLL_MILLIS = 15L
        private const val MAX_WRITE_STALL_MILLIS = 2_000L
    }

    private val chunks = Channel<ByteArray>(MAX_QUEUED_CHUNKS)
    private val queuedBytes = AtomicInteger()

    fun enqueue(chunk: ByteArray): Boolean {
        if (queuedBytes.addAndGet(chunk.size) <= MAX_QUEUED_BYTES && chunks.trySend(chunk).isSuccess) return true
        queuedBytes.addAndGet(-chunk.size)
        return false
    }

    suspend fun play(
        attributes: AudioAttributes,
        isMediaCurrent: () -> Boolean,
        getLevel: (ByteArray) -> Float,
        onPlayback: (Boolean, Float) -> Unit
    ) {
        val track = createTrack(attributes)
        try {
            check(track.state == AudioTrack.STATE_INITIALIZED)
            track.play()
            check(track.playState == AudioTrack.PLAYSTATE_PLAYING)
            writeChunks(track, isMediaCurrent, getLevel, onPlayback)
        } finally {
            runCatching { track.pause() }
            runCatching { track.flush() }
            track.release()
        }
    }

    fun close() {
        chunks.cancel()
    }

    private fun createTrack(attributes: AudioAttributes): AudioTrack {
        val minimum = AudioTrack.getMinBufferSize(
            AppConstant.CALL_OUTPUT_SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        check(minimum > 0)
        val format = AudioFormat.Builder()
            .setSampleRate(AppConstant.CALL_OUTPUT_SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .build()
        return AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(format)
            .setBufferSizeInBytes(minimum)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()
    }

    private suspend fun writeChunks(
        track: AudioTrack,
        isMediaCurrent: () -> Boolean,
        getLevel: (ByteArray) -> Float,
        onPlayback: (Boolean, Float) -> Unit
    ) {
        var chunk: ByteArray? = null
        var offset = 0
        var level = 0f
        var framesWritten = 0L
        var lastWriteAtMillis = SystemClock.elapsedRealtime()
        while (currentCoroutineContext().isActive && isMediaCurrent()) {
            if (chunk == null) {
                chunk = chunks.tryReceive().getOrNull()
                offset = 0
                lastWriteAtMillis = SystemClock.elapsedRealtime()
            }
            val currentChunk = chunk
            var written = 0
            if (currentChunk != null) {
                written = track.write(currentChunk, offset, currentChunk.size - offset, AudioTrack.WRITE_NON_BLOCKING)
                check(written >= 0)
                if (written > 0) {
                    if (offset == 0) level = getLevel(currentChunk)
                    framesWritten += written / AppConstant.PCM16_BYTES_PER_SAMPLE
                    lastWriteAtMillis = SystemClock.elapsedRealtime()
                    offset += written
                    if (offset == currentChunk.size) {
                        queuedBytes.addAndGet(-currentChunk.size)
                        chunk = null
                    }
                } else {
                    check(SystemClock.elapsedRealtime() - lastWriteAtMillis < MAX_WRITE_STALL_MILLIS)
                }
            }
            val framesPlayed = track.playbackHeadPosition.toUInt().toLong()
            val isPlaying = framesPlayed > 0 && framesPlayed < framesWritten
            onPlayback(isPlaying, if (isPlaying) level else 0f)
            if (written == 0) delay(PLAYBACK_POLL_MILLIS)
        }
    }
}
