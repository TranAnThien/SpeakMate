package com.project.speakmate.data.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import com.project.speakmate.utils.AppConstant
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class AudioCapture {
    companion object {
        private const val CHUNK_MILLIS = 40
        private const val MILLIS_PER_SECOND = 1_000
        private const val READ_POLL_MILLIS = 10L
        private const val MAX_READ_STALL_MILLIS = 2_000L
        private const val BUFFER_CHUNKS = 4
        private const val CHUNK_SAMPLES = AppConstant.CALL_INPUT_SAMPLE_RATE * CHUNK_MILLIS / MILLIS_PER_SECOND
        private const val CHUNK_BYTES = CHUNK_SAMPLES * AppConstant.PCM16_BYTES_PER_SAMPLE
    }

    suspend fun record(onChunk: (ByteArray) -> Unit) {
        val recorder = createRecorder()
        try {
            check(recorder.state == AudioRecord.STATE_INITIALIZED)
            recorder.startRecording()
            check(recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING)
            readChunks(recorder, onChunk)
        } finally {
            runCatching { recorder.stop() }
            recorder.release()
        }
    }

    @SuppressLint("MissingPermission")
    private fun createRecorder(): AudioRecord {
        val minimum = AudioRecord.getMinBufferSize(
            AppConstant.CALL_INPUT_SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        check(minimum > 0)
        val format = AudioFormat.Builder()
            .setSampleRate(AppConstant.CALL_INPUT_SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .build()
        return AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
            .setAudioFormat(format)
            .setBufferSizeInBytes(maxOf(minimum, CHUNK_BYTES * BUFFER_CHUNKS))
            .build()
    }

    private suspend fun readChunks(recorder: AudioRecord, onChunk: (ByteArray) -> Unit) {
        val samples = ShortArray(CHUNK_SAMPLES)
        val chunk = ByteArray(CHUNK_BYTES)
        var offset = 0
        var lastReadAtMillis = SystemClock.elapsedRealtime()
        while (currentCoroutineContext().isActive) {
            val count = recorder.read(samples, offset, samples.size - offset, AudioRecord.READ_NON_BLOCKING)
            check(count >= 0)
            if (count == 0) {
                check(SystemClock.elapsedRealtime() - lastReadAtMillis < MAX_READ_STALL_MILLIS)
                delay(READ_POLL_MILLIS)
                continue
            }
            lastReadAtMillis = SystemClock.elapsedRealtime()
            offset += count
            if (offset == samples.size) {
                for (index in samples.indices) {
                    val sample = samples[index].toInt()
                    val byteIndex = index * AppConstant.PCM16_BYTES_PER_SAMPLE
                    chunk[byteIndex] = sample.toByte()
                    chunk[byteIndex + 1] = (sample shr AppConstant.BITS_PER_BYTE).toByte()
                }
                onChunk(chunk)
                offset = 0
            }
        }
    }
}
