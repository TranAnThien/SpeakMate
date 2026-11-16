package com.project.speakmate.domain.model

enum class SpeakingLevel {
    NOT_ASSESSED,
    A1,
    A2,
    B1,
    B2;

    fun getNextLevel(): SpeakingLevel? = when (this) {
        NOT_ASSESSED, B2 -> null
        A1 -> A2
        A2 -> B1
        B1 -> B2
    }
}
