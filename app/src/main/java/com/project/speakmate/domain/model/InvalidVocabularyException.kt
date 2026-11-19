package com.project.speakmate.domain.model

class InvalidVocabularyException(val reason: Reason) : IllegalArgumentException(if (reason == Reason.INVALID_CONTENT) EMPTY_MESSAGE else reason.name) {
    companion object {
        private const val EMPTY_MESSAGE = "Word and mean cannot be empty"
    }

    enum class Reason {
        INVALID_CONTENT,
        FIELD_TOO_LONG,
        STORAGE_BUDGET
    }
}
