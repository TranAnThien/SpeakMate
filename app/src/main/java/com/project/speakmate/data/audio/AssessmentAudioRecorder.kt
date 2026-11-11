package com.project.speakmate.data.audio

import com.project.speakmate.utils.AppConstant
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

class AssessmentAudioRecorder(private val file: File) {
    companion object {
        const val MAX_ASSESSMENT_AUDIO_MILLIS = 5 * 60 * 1_000L
        private const val MILLIS_PER_SECOND = 1_000L
        private const val HEADER_BYTES = 44
        private const val RIFF_CONTENT_BYTES = 36
        private const val FORMAT_BYTES = 16
        private const val PCM_FORMAT = 1
        private const val CHANNELS = 1
        private const val RIFF = "RIFF"
        private const val WAVE = "WAVE"
        private const val FORMAT = "fmt "
        private const val DATA = "data"
        private const val READ_WRITE_MODE = "rw"
        private const val BYTES_PER_SECOND = AppConstant.CALL_INPUT_SAMPLE_RATE * AppConstant.PCM16_BYTES_PER_SAMPLE * CHANNELS
        private const val MAX_AUDIO_BYTES = MAX_ASSESSMENT_AUDIO_MILLIS * BYTES_PER_SECOND / MILLIS_PER_SECOND
    }

    private var output: RandomAccessFile? = null
    private var audioBytes = 0L
    private var isFinished = false

    val durationMillis: Long
        @Synchronized get() = audioBytes * MILLIS_PER_SECOND / BYTES_PER_SECOND

    val isAtLimit: Boolean
        @Synchronized get() = audioBytes >= MAX_AUDIO_BYTES

    val isCapturing: Boolean
        @Synchronized get() = output != null

    init {
        require(file.createNewFile())
        val current = try {
            RandomAccessFile(file, READ_WRITE_MODE)
        } catch (failure: Exception) {
            file.delete()
            throw failure
        }
        try {
            current.setLength(HEADER_BYTES.toLong())
            output = current
        } catch (failure: Exception) {
            try {
                current.close()
            } finally {
                file.delete()
                throw failure
            }
        }
    }

    @Synchronized
    fun canAppend(chunk: ByteArray): Boolean =
        output != null && chunk.size % AppConstant.PCM16_BYTES_PER_SAMPLE == 0 && audioBytes + chunk.size <= MAX_AUDIO_BYTES

    @Synchronized
    fun append(chunk: ByteArray): Boolean {
        if (!canAppend(chunk)) return false
        val current = output ?: return false
        current.seek(HEADER_BYTES + audioBytes)
        current.write(chunk)
        audioBytes += chunk.size
        return true
    }

    @Synchronized
    fun resumeCapture() {
        if (isFinished || output != null) return
        check(file.isFile && file.length() == HEADER_BYTES + audioBytes)
        output = RandomAccessFile(file, READ_WRITE_MODE)
    }

    @Synchronized
    fun pauseCapture() {
        closeOutput()
    }

    @Synchronized
    fun close() {
        if (isFinished) return
        isFinished = true
        try {
            closeOutput()
        } finally {
            if (audioBytes == 0L) file.delete()
        }
    }

    @Synchronized
    fun delete() {
        try {
            close()
        } finally {
            check(!file.exists() || file.delete())
        }
    }

    private fun closeOutput() {
        val current = output ?: return
        output = null
        try {
            current.seek(0L)
            current.write(createHeader())
        } finally {
            current.close()
        }
    }

    private fun createHeader(): ByteArray = ByteBuffer.allocate(HEADER_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        .put(RIFF.toByteArray(StandardCharsets.US_ASCII))
        .putInt(RIFF_CONTENT_BYTES + audioBytes.toInt())
        .put(WAVE.toByteArray(StandardCharsets.US_ASCII))
        .put(FORMAT.toByteArray(StandardCharsets.US_ASCII))
        .putInt(FORMAT_BYTES)
        .putShort(PCM_FORMAT.toShort())
        .putShort(CHANNELS.toShort())
        .putInt(AppConstant.CALL_INPUT_SAMPLE_RATE)
        .putInt(BYTES_PER_SECOND)
        .putShort((CHANNELS * AppConstant.PCM16_BYTES_PER_SAMPLE).toShort())
        .putShort((AppConstant.PCM16_BYTES_PER_SAMPLE * AppConstant.BITS_PER_BYTE).toShort())
        .put(DATA.toByteArray(StandardCharsets.US_ASCII))
        .putInt(audioBytes.toInt())
        .array()
}
