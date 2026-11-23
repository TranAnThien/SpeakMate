package com.project.speakmate.data.audio

import java.io.File
import java.util.UUID

class AssessmentAudioFileProvider(private val directory: File, private val legacyDirectory: File? = null) {
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
        legacyDirectory?.let { legacy ->
            legacy.listFiles()?.filter { file -> file.isFile && file.canonicalFile.parentFile == legacy.canonicalFile &&
                file.name.endsWith(EXTENSION) && file.name.removeSuffix(EXTENSION).split(SEPARATOR).let { parts -> parts.size == FILE_NAME_PARTS && parts.all(::isValidId) }
            }?.forEach { check(it.delete() || !it.exists()) }
        }
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

    private fun isValidId(audioId: String): Boolean = try {
        UUID.fromString(audioId).toString() == audioId
    } catch (_: IllegalArgumentException) {
        false
    }
}
