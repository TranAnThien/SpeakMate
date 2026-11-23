package com.project.speakmate.domain.rules

import com.project.speakmate.domain.model.PracticeAttempt

object PracticeCommitRules {
    fun hasValidTranscripts(attempt: PracticeAttempt): Boolean = attempt.transcripts.let { messages ->
        messages.map { it.messageId }.distinct().size == messages.size && messages.map { it.orderIndex }.distinct().size == messages.size &&
            messages.all { it.messageId.isNotBlank() && !it.messageId.contains('/') && it.isFinal && it.orderIndex >= 0 }
    }

    fun matches(expected: PracticeAttempt, actual: PracticeAttempt): Boolean = hasValidTranscripts(expected) && hasValidTranscripts(actual) &&
        expected.copy(transcripts = emptyList()) == actual.copy(transcripts = emptyList()) &&
        expected.transcripts.sortedBy { it.orderIndex } == actual.transcripts.sortedBy { it.orderIndex }
}
