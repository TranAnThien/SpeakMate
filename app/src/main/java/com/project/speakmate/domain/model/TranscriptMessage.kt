package com.project.speakmate.domain.model

data class TranscriptMessage(
    val messageId: String,
    val senderType: SenderType,
    val content: String,
    val orderIndex: Int,
    val isFinal: Boolean,
    val isInterrupted: Boolean
)
