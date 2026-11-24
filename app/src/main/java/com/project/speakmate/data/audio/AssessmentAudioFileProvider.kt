package com.project.speakmate.data.audio

import java.io.File
import java.util.UUID

class AssessmentAudioFileProvider(private val directory: File, private val legacyDirectory: File? = null) {
    companion object {
        private const val EXTENSION = ".wav"
        private const val SEPARATOR = "_"
        private const val FILE_NAME_PARTS = 2
        private const val CLEANUP_FAILED = "Temporary audio cleanup failed"
    }

    private val processId = UUID.randomUUID().toString()

    fun createAudioId(): String = UUID.randomUUID().toString()

    fun openRecorder(audioId: String): AssessmentAudioRecorder {
        require(isValidId(audioId))
        check(directory.isDirectory || directory.mkdirs())
        return AssessmentAudioRecorder(getOwnedFile(audioId))
    }

    @Synchronized
    fun getAudioFile(audioId: String): File? {
        if (!isValidId(audioId)) return null
        val file = getOwnedFile(audioId)
        return file.takeIf { it.isFile && it.length() > 0L }
    }

    @Synchronized
    fun deleteAudio(audioId: String) {
        if (!isValidId(audioId)) return
        val file = getOwnedFile(audioId)
        if (file.isFile) check(!file.exists() || file.delete())
    }

    @Synchronized
    fun clearPreviousProcessFiles() {
        val failures = mutableListOf<Exception>()
        for (parent in listOfNotNull(legacyDirectory, directory).distinct()) {
            for (file in parent.listFiles().orEmpty()) {
                try {
                    if (isPreviousProcessFile(file, parent)) check(!file.exists() || file.delete())
                } catch (failure: Exception) {
                    failures.add(failure)
                }
            }
        }
        if (failures.isNotEmpty()) {
            throw IllegalStateException(CLEANUP_FAILED).also { failure -> failures.forEach(failure::addSuppressed) }
        }
    }

    private fun isPreviousProcessFile(file: File, parent: File): Boolean {
        if (!file.isFile || file.canonicalFile.parentFile != parent.canonicalFile) return false
        if (!file.name.endsWith(EXTENSION)) return false
        val parts = file.name.removeSuffix(EXTENSION).split(SEPARATOR)
        return parts.size == FILE_NAME_PARTS && parts.all(::isValidId) && (parent != directory || parts[0] != processId)
    }

    private fun getOwnedFile(audioId: String): File {
        val file = File(directory, processId + SEPARATOR + audioId + EXTENSION)
        check(file.canonicalFile.parentFile == directory.canonicalFile)
        return file
    }

    private fun isValidId(audioId: String): Boolean =
        try {
            UUID.fromString(audioId).toString() == audioId
        } catch (_: IllegalArgumentException) {
            false
        }
}
