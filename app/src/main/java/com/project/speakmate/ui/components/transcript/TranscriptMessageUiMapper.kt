package com.project.speakmate.ui.components.transcript

import com.project.speakmate.domain.model.SenderType
import com.project.speakmate.domain.model.TranscriptMessage

object TranscriptMessageUiMapper {
    fun toMessages(messages: List<TranscriptMessage>): List<TranscriptMessageUiState> =
        messages
            .filter { it.isFinal && it.content.isNotBlank() }
            .sortedWith(compareBy<TranscriptMessage> { it.orderIndex }.thenBy { it.messageId })
            .distinctBy { it.messageId }
            .map {
                TranscriptMessageUiState(
                    id = it.messageId,
                    sender = it.senderType,
                    isInterrupted = it.senderType == SenderType.AI && it.isInterrupted,
                    text = it.content,
                    isUser = it.senderType == SenderType.USER
                )
            }
}
