package com.project.speakmate.data.audio

import java.util.UUID
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AssessmentAudioFileProviderTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun audioIdsAreOpaqueRandomUuids() {
        val files = AssessmentAudioFileProvider(folder.root)
        val first = files.createAudioId()
        assertEquals(first, UUID.fromString(first).toString())
        assertNotEquals(first, files.createAudioId())
    }

    @Test
    fun findsOnlyOwnedAudioAndDeletesRepeatedly() {
        val files = AssessmentAudioFileProvider(folder.root)
        val audioId = files.createAudioId()
        val recorder = files.openRecorder(audioId)
        recorder.append(byteArrayOf(0, 0))
        recorder.close()
        val file = checkNotNull(files.getAudioFile(audioId))
        assertEquals(folder.root.canonicalFile, file.canonicalFile.parentFile)
        files.deleteAudio(audioId)
        files.deleteAudio(audioId)
        assertFalse(file.exists())
    }

    @Test
    fun unknownAndPathIdsCannotReadOrDeleteOtherFiles() {
        val files = AssessmentAudioFileProvider(folder.newFolder("assessment"))
        val outside = folder.newFile("outside.wav")
        outside.writeText("outside")
        listOf("", "../outside.wav", "..\\outside.wav", outside.absolutePath, "file:///outside.wav", files.createAudioId()).forEach {
            assertNull(files.getAudioFile(it))
            files.deleteAudio(it)
        }
        assertEquals("outside", outside.readText())
    }

    @Test
    fun startupCleanupRemovesPreviousProcessButKeepsNewEvidence() {
        val oldProcess = AssessmentAudioFileProvider(folder.root)
        val oldId = oldProcess.createAudioId()
        oldProcess.openRecorder(oldId).also { it.append(byteArrayOf(0, 0)); it.close() }
        val oldFile = checkNotNull(oldProcess.getAudioFile(oldId))
        val newProcess = AssessmentAudioFileProvider(folder.root)
        val newId = newProcess.createAudioId()
        newProcess.openRecorder(newId).also { it.append(byteArrayOf(0, 0)); it.close() }
        newProcess.clearPreviousProcessFiles()
        newProcess.clearPreviousProcessFiles()
        assertFalse(oldFile.exists())
        assertTrue(newProcess.getAudioFile(newId) != null)
        assertNull(newProcess.getAudioFile(oldId))
    }

    @Test
    fun openingNewRecorderRemovesCurrentProcessOrphanAndKeepsNewFile() {
        val files = AssessmentAudioFileProvider(folder.root)
        val orphanId = files.createAudioId()
        files.openRecorder(orphanId).also { it.append(byteArrayOf(0, 0)); it.close() }
        val orphan = checkNotNull(files.getAudioFile(orphanId))
        val newId = files.createAudioId()
        val recorder = files.openRecorder(newId)
        try {
            assertFalse(orphan.exists())
            assertTrue(files.getAudioFile(newId) != null)
            assertTrue(recorder.append(byteArrayOf(0, 0)))
        } finally {
            recorder.close()
        }
    }

    @Test
    fun openingRecorderDoesNotDeleteMalformedNamesOrPreviousProcessFiles() {
        val previous = AssessmentAudioFileProvider(folder.root)
        val previousId = previous.createAudioId()
        previous.openRecorder(previousId).also { it.append(byteArrayOf(0, 0)); it.close() }
        val previousFile = checkNotNull(previous.getAudioFile(previousId))
        val files = AssessmentAudioFileProvider(folder.root)
        val firstId = files.createAudioId()
        files.openRecorder(firstId).also { it.append(byteArrayOf(0, 0)); it.close() }
        val processPrefix = checkNotNull(files.getAudioFile(firstId)).name.substringBefore('_')
        val malformed = listOf(
            File(folder.root, "${processPrefix}_not-a-uuid.wav"),
            File(folder.root, "not-a-uuid_${files.createAudioId()}.wav"),
            File(folder.root, "${processPrefix}_${files.createAudioId()}.txt")
        )
        malformed.forEach { it.writeText("keep") }
        val newId = files.createAudioId()
        files.openRecorder(newId).also { it.append(byteArrayOf(0, 0)); it.close() }
        malformed.forEach { assertEquals("keep", it.readText()) }
        assertTrue(previousFile.exists())
        assertTrue(files.getAudioFile(newId) != null)
    }
}
