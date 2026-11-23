package com.project.speakmate.data.repository

import com.project.speakmate.domain.model.TranscriptMessage

object HistoryTranscriptMapper {
    fun toMessages(messages: List<TranscriptMessage>): List<TranscriptMessage> {
        val finalMessages = messages.filter { it.isFinal && it.content.isNotBlank() }
        require(finalMessages.all { it.messageId.isNotBlank() && it.orderIndex >= 0 })
        require(finalMessages.map { it.messageId }.distinct().size == finalMessages.size)
        return finalMessages.sortedWith(compareBy<TranscriptMessage> { it.orderIndex }.thenBy { it.messageId })
    }
}
