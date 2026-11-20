package com.project.speakmate.data.session

import com.project.speakmate.data.audio.AssessmentAudioFileProvider
import com.project.speakmate.data.audio.AssessmentAudioRecorder
import com.project.speakmate.domain.model.CallActivityType
import com.project.speakmate.domain.model.CallOutcome
import com.project.speakmate.utils.AppConstant
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CallAssessmentRecordingTest {
    companion object {
        private const val BYTES_PER_SECOND = AppConstant.CALL_INPUT_SAMPLE_RATE * AppConstant.PCM16_BYTES_PER_SAMPLE
        private const val ONE_SECOND_MILLIS = 1_000L
        private const val SYNTHETIC_AUDIO_ID = "audio-id"
    }

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun freePracticeNeverCreatesAssessmentEvidence() {
        val files = AssessmentAudioFileProvider(folder.root)
        val recording = CallAssessmentRecording(files)
        recording.configureAssessmentRecording(CallActivityType.FREE_PRACTICE) {}
        assertNull(recording.resumeCapture())
        recording.finishAssessmentRecording(CallOutcome.COMPLETED)
        assertNull(recording.assessmentAudioId)
        assertEquals(0, checkNotNull(folder.root.listFiles()).size)
    }

    @Test
    fun completedAttemptRetainsEvidenceAndDuplicateFinishPreservesIt() {
        val files = AssessmentAudioFileProvider(folder.root)
        val recording = createRecording(files)
        val recorder = checkNotNull(recording.resumeCapture())
        recording.append(recorder, ByteArray(BYTES_PER_SECOND))
        recording.finishAssessmentRecording(CallOutcome.COMPLETED)
        val audioId = checkNotNull(recording.assessmentAudioId)
        val file = checkNotNull(files.getAudioFile(audioId))
        val size = file.length()
        assertEquals(ONE_SECOND_MILLIS, recording.assessmentAudioDurationMillis)
        recording.finishAssessmentRecording(CallOutcome.FAILED)
        assertEquals(audioId, recording.assessmentAudioId)
        assertTrue(file.exists())
        assertEquals(size, file.length())
    }

    @Test
    fun cancelledAndFailedAttemptsDeleteEvidenceWithoutPublishingAudioId() {
        listOf(CallOutcome.CANCELLED, CallOutcome.FAILED).forEach { outcome ->
            val directory = folder.newFolder()
            val recording = createRecording(AssessmentAudioFileProvider(directory))
            recording.append(checkNotNull(recording.resumeCapture()), ByteArray(BYTES_PER_SECOND))
            recording.finishAssessmentRecording(outcome)
            recording.finishAssessmentRecording(outcome)
            assertNull(recording.assessmentAudioId)
            assertNull(recording.assessmentAudioDurationMillis)
            assertEquals(0, checkNotNull(directory.listFiles()).size)
        }
    }

    @Test
    fun emptyCompletedRecordingIsNotPublished() {
        val recording = createRecording(AssessmentAudioFileProvider(folder.root))
        recording.resumeCapture()
        recording.finishAssessmentRecording(CallOutcome.COMPLETED)
        assertNull(recording.assessmentAudioId)
        assertEquals(0, checkNotNull(folder.root.listFiles()).size)
    }

    @Test
    fun pauseAndResumeReuseOneAttemptAndRejectPausedChunksWithoutReportingLimit() {
        var limits = 0
        val recording = createRecording(AssessmentAudioFileProvider(folder.root)) { limits++ }
        val recorder = checkNotNull(recording.resumeCapture())
        recording.append(recorder, ByteArray(BYTES_PER_SECOND))
        recording.pauseCapture()
        assertFalse(recording.canAppend(recorder, byteArrayOf(0, 0)))
        assertEquals(0, limits)
        assertSame(recorder, recording.resumeCapture())
        recording.append(recorder, ByteArray(BYTES_PER_SECOND))
        recording.finishAssessmentRecording(CallOutcome.COMPLETED)
        assertEquals(ONE_SECOND_MILLIS * 2, recording.assessmentAudioDurationMillis)
    }

    @Test
    fun assessmentLimitNotifiesOnceAndRejectsFurtherAudio() {
        var limits = 0
        val recording = createRecording(AssessmentAudioFileProvider(folder.root)) { limits++ }
        val recorder = checkNotNull(recording.resumeCapture())
        val maximumBytes = (BYTES_PER_SECOND * AssessmentAudioRecorder.MAX_ASSESSMENT_AUDIO_MILLIS / ONE_SECOND_MILLIS).toInt()
        recording.append(recorder, ByteArray(maximumBytes))
        repeat(2) { assertFalse(recording.canAppend(recorder, byteArrayOf(0, 0))) }
        assertEquals(1, limits)
        recording.finishAssessmentRecording(CallOutcome.COMPLETED)
        assertEquals(AssessmentAudioRecorder.MAX_ASSESSMENT_AUDIO_MILLIS, recording.assessmentAudioDurationMillis)
    }

    @Test
    fun appendFailurePreventsPublishingAnOtherwiseCompletedRecording() {
        val recorder = mockk<AssessmentAudioRecorder>(relaxed = true)
        every { recorder.append(any()) } returns false
        val recording = createMockRecording(recorder)
        recording.resumeCapture()
        assertThrows(IOException::class.java) { recording.append(recorder, byteArrayOf(0, 0)) }
        assertThrows(IOException::class.java) { recording.finishAssessmentRecording(CallOutcome.COMPLETED) }
        recording.finishAssessmentRecording(CallOutcome.COMPLETED)
        assertNull(recording.assessmentAudioId)
        verify(exactly = 1) { recorder.delete() }
    }

    @Test
    fun cancelledAppendPropagatesTheOriginalCancellation() {
        val recorder = mockk<AssessmentAudioRecorder>(relaxed = true)
        val cancellation = CancellationException("cancelled")
        every { recorder.append(any()) } throws cancellation
        val recording = createMockRecording(recorder)
        recording.resumeCapture()
        assertSame(cancellation, assertThrows(CancellationException::class.java) { recording.append(recorder, byteArrayOf(0, 0)) })
        recording.finishAssessmentRecording(CallOutcome.CANCELLED)
        assertNull(recording.assessmentAudioId)
    }

    @Test
    fun closeCancellationDeletesEvidenceAndPreservesSuppressedCleanupFailure() {
        val recorder = mockk<AssessmentAudioRecorder>(relaxed = true)
        val cancellation = CancellationException("cancelled")
        val cleanup = IOException("delete failed")
        every { recorder.close() } throws cancellation
        every { recorder.delete() } throws cleanup
        val recording = createMockRecording(recorder)
        recording.resumeCapture()
        assertSame(cancellation, assertThrows(CancellationException::class.java) { recording.finishAssessmentRecording(CallOutcome.COMPLETED) })
        assertEquals(listOf(cleanup), cancellation.suppressed.toList())
        recording.finishAssessmentRecording(CallOutcome.COMPLETED)
        verify(exactly = 1) { recorder.close() }
    }

    @Test
    fun separateAttemptDoesNotInheritPublishedMetadataOrFailureState() {
        val first = createRecording(AssessmentAudioFileProvider(folder.newFolder()))
        first.append(checkNotNull(first.resumeCapture()), ByteArray(BYTES_PER_SECOND))
        first.finishAssessmentRecording(CallOutcome.COMPLETED)
        assertNotNull(first.assessmentAudioId)
        val second = createRecording(AssessmentAudioFileProvider(folder.newFolder()))
        assertNull(second.assessmentAudioId)
        assertNull(second.assessmentAudioDurationMillis)
        second.finishAssessmentRecording(CallOutcome.CANCELLED)
        assertNotNull(first.assessmentAudioId)
    }

    private fun createRecording(files: AssessmentAudioFileProvider, onLimit: () -> Unit = {}): CallAssessmentRecording =
        CallAssessmentRecording(files).also { it.configureAssessmentRecording(CallActivityType.TRAINING_UNIT, onLimit) }

    private fun createMockRecording(recorder: AssessmentAudioRecorder): CallAssessmentRecording {
        val files = mockk<AssessmentAudioFileProvider>()
        every { files.createAudioId() } returns SYNTHETIC_AUDIO_ID
        every { files.openRecorder(SYNTHETIC_AUDIO_ID) } returns recorder
        return createRecording(files)
    }
}
