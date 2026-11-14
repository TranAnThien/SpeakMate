package com.project.speakmate.data.audio

import com.project.speakmate.utils.AppConstant
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AssessmentAudioRecorderTest {
    companion object {
        private const val MILLIS_PER_SECOND = 1_000L
        private const val HEADER_BYTES = 44
        private const val BYTES_PER_SECOND = AppConstant.CALL_INPUT_SAMPLE_RATE * AppConstant.PCM16_BYTES_PER_SAMPLE
    }

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun writesPcm16MonoWavHeaderAndDuration() {
        val file = File(folder.root, "evidence.wav")
        val recorder = AssessmentAudioRecorder(file)
        val samples = ByteArray(BYTES_PER_SECOND) { (it % Byte.MAX_VALUE).toByte() }
        assertTrue(recorder.append(samples))
        recorder.close()

        val bytes = file.readBytes()
        val header = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals("RIFF", String(bytes, 0, 4, Charsets.US_ASCII))
        assertEquals(bytes.size - 8, header.getInt(4))
        assertEquals("WAVE", String(bytes, 8, 4, Charsets.US_ASCII))
        assertEquals("fmt ", String(bytes, 12, 4, Charsets.US_ASCII))
        assertEquals(16, header.getInt(16))
        assertEquals(1, header.getShort(20).toInt())
        assertEquals(1, header.getShort(22).toInt())
        assertEquals(AppConstant.CALL_INPUT_SAMPLE_RATE, header.getInt(24))
        assertEquals(BYTES_PER_SECOND, header.getInt(28))
        assertEquals(AppConstant.PCM16_BYTES_PER_SAMPLE, header.getShort(32).toInt())
        assertEquals(16, header.getShort(34).toInt())
        assertEquals("data", String(bytes, 36, 4, Charsets.US_ASCII))
        assertEquals(samples.size, header.getInt(40))
        assertArrayEquals(samples, bytes.copyOfRange(HEADER_BYTES, bytes.size))
        assertEquals(MILLIS_PER_SECOND, recorder.durationMillis)
    }

    @Test
    fun closingEmptyEvidenceDeletesFile() {
        val file = File(folder.root, "empty.wav")
        AssessmentAudioRecorder(file).close()
        assertFalse(file.exists())
    }

    @Test
    fun closingAndDeletingAreRepeatSafe() {
        val file = File(folder.root, "repeat.wav")
        val recorder = AssessmentAudioRecorder(file)
        recorder.append(ByteArray(BYTES_PER_SECOND))
        recorder.close()
        val length = file.length()
        recorder.close()
        assertEquals(length, file.length())
        recorder.delete()
        recorder.delete()
        assertFalse(file.exists())
    }

    @Test
    fun staleAppendAfterCloseCannotChangeEvidence() {
        val file = File(folder.root, "closed.wav")
        val recorder = AssessmentAudioRecorder(file)
        recorder.append(ByteArray(BYTES_PER_SECOND))
        recorder.close()
        assertFalse(recorder.append(ByteArray(BYTES_PER_SECOND)))
        assertEquals(HEADER_BYTES + BYTES_PER_SECOND.toLong(), file.length())
    }

    @Test
    fun capsEvidenceAtFiveMinutes() {
        val file = File(folder.root, "limit.wav")
        val recorder = AssessmentAudioRecorder(file)
        val seconds = AssessmentAudioRecorder.MAX_ASSESSMENT_AUDIO_MILLIS / MILLIS_PER_SECOND
        repeat(seconds.toInt()) { assertTrue(recorder.append(ByteArray(BYTES_PER_SECOND))) }
        assertTrue(recorder.isAtLimit)
        assertFalse(recorder.canAppend(byteArrayOf(0, 0)))
        assertFalse(recorder.append(byteArrayOf(0, 0)))
        recorder.close()
        assertEquals(AssessmentAudioRecorder.MAX_ASSESSMENT_AUDIO_MILLIS, recorder.durationMillis)
        assertEquals(HEADER_BYTES + seconds * BYTES_PER_SECOND, file.length())
    }

    @Test
    fun rejectsMisalignedPcmWithoutWriting() {
        val file = File(folder.root, "unaligned.wav")
        val recorder = AssessmentAudioRecorder(file)
        assertFalse(recorder.append(byteArrayOf(0)))
        assertEquals(0L, recorder.durationMillis)
        recorder.close()
        assertFalse(file.exists())
    }

    @Test
    fun pauseClosesHandleAndResumeAppendsToSameWav() {
        val file = File(folder.root, "paused.wav")
        val recorder = AssessmentAudioRecorder(file)
        recorder.append(byteArrayOf(1, 2))
        recorder.pauseCapture()
        assertFalse(recorder.isCapturing)
        assertFalse(recorder.append(byteArrayOf(3, 4)))
        recorder.resumeCapture()
        assertTrue(recorder.isCapturing)
        recorder.append(byteArrayOf(5, 6))
        recorder.close()
        recorder.resumeCapture()
        assertFalse(recorder.append(byteArrayOf(7, 8)))
        assertArrayEquals(byteArrayOf(1, 2, 5, 6), file.readBytes().drop(HEADER_BYTES).toByteArray())
    }

    @Test
    fun separateAttemptsUseSeparateFiles() {
        val firstFile = File(folder.root, "first.wav")
        val secondFile = File(folder.root, "second.wav")
        val first = AssessmentAudioRecorder(firstFile)
        first.append(ByteArray(BYTES_PER_SECOND))
        first.close()
        val second = AssessmentAudioRecorder(secondFile)
        second.append(byteArrayOf(1, 2))
        assertFalse(first.append(byteArrayOf(3, 4)))
        second.close()
        assertEquals(HEADER_BYTES + BYTES_PER_SECOND.toLong(), firstFile.length())
        assertArrayEquals(byteArrayOf(1, 2), secondFile.readBytes().drop(HEADER_BYTES).toByteArray())
    }
}
