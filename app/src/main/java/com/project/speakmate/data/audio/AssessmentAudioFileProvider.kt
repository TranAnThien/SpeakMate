package com.project.speakmate.data.audio

import java.io.File
import java.util.UUID

class AssessmentAudioFileProvider(private val directory: File) {
    companion object {
        private const val EXTENSION = ".wav"
        private const val SEPARATOR = "_"
        private const val FILE_NAME_PARTS = 2
    }

    private val processId = UUID.randomUUID().toString()

    fun createAudioId(): String = UUID.randomUUID().toString()

    fun openRecorder(audioId: String): AssessmentAudioRecorder {
        require(isValidId(audioId))
        check(directory.isDirectory || directory.mkdirs())
        val recorder = AssessmentAudioRecorder(getOwnedFile(audioId))
        try {
            clearOtherCurrentProcessFiles(audioId)
            return recorder
        } catch (failure: Exception) {
            try {
                recorder.delete()
            } catch (cleanupFailure: Exception) {
                failure.addSuppressed(cleanupFailure)
            }
            throw failure
        }
    }

    fun getAudioFile(audioId: String): File? {
        if (!isValidId(audioId)) return null
        val file = getOwnedFile(audioId)
        return file.takeIf { it.isFile && it.length() > 0L }
    }

    fun deleteAudio(audioId: String) {
        if (!isValidId(audioId)) return
        val file = getOwnedFile(audioId)
        if (!file.isFile) return
        check(!file.exists() || file.delete())
    }

    fun clearPreviousProcessFiles() {
        val files = directory.listFiles() ?: return
        for (file in files) {
            if (!file.isFile || file.canonicalFile.parentFile != directory.canonicalFile) continue
            val name = file.name.removeSuffix(EXTENSION)
            if (!file.name.endsWith(EXTENSION)) continue
            val parts = name.split(SEPARATOR)
            if (parts.size != FILE_NAME_PARTS || !parts.all(::isValidId) || parts[0] == processId) continue
            check(!file.exists() || file.delete())
        }
    }

    private fun getOwnedFile(audioId: String): File {
        val file = File(directory, processId + SEPARATOR + audioId + EXTENSION)
        check(file.canonicalFile.parentFile == directory.canonicalFile)
        return file
    }

    private fun clearOtherCurrentProcessFiles(audioId: String) {
        val files = directory.listFiles() ?: return
        for (file in files) {
            if (!file.isFile || file.canonicalFile.parentFile != directory.canonicalFile) continue
            if (!file.name.endsWith(EXTENSION)) continue
            val parts = file.name.removeSuffix(EXTENSION).split(SEPARATOR)
            if (parts.size != FILE_NAME_PARTS || !parts.all(::isValidId) || parts[0] != processId || parts[1] == audioId) continue
            check(!file.exists() || file.delete())
        }
    }

    private fun isValidId(audioId: String): Boolean = try {
        UUID.fromString(audioId).toString() == audioId
    } catch (_: IllegalArgumentException) {
        false
    }
}
