package com.project.speakmate.data.dto

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.project.speakmate.data.remote.firebase.FirestoreField
import com.project.speakmate.domain.model.SenderType
import com.project.speakmate.domain.model.TranscriptMessage
import com.project.speakmate.utils.extension.default

@IgnoreExtraProperties
data class TranscriptMessageDto(
    val messageId: String = "",
    val senderType: String = "",
    val content: String = "",
    val orderIndex: Int = 0,
    @get:PropertyName(FirestoreField.IS_FINAL)
    @set:PropertyName(FirestoreField.IS_FINAL)
    var isFinal: Boolean? = null,
    @get:PropertyName(FirestoreField.IS_INTERRUPTED)
    @set:PropertyName(FirestoreField.IS_INTERRUPTED)
    var isInterrupted: Boolean? = null
)

fun TranscriptMessage.toDto(): TranscriptMessageDto {
    return TranscriptMessageDto(
        messageId = this.messageId,
        senderType = this.senderType.name,
        content = this.content,
        orderIndex = this.orderIndex,
        isFinal = this.isFinal,
        isInterrupted = this.isInterrupted
    )
}

fun TranscriptMessageDto.toDomain(
    legacyFinal: Boolean? = null,
    legacyInterrupted: Boolean? = null
): TranscriptMessage? {
    val sender = SenderType.entries.firstOrNull { it.name == senderType } ?: return null
    return TranscriptMessage(
        messageId = this.messageId,
        senderType = sender,
        content = this.content,
        orderIndex = this.orderIndex,
        isFinal = isFinal.default(legacyFinal.default(true)),
        isInterrupted = isInterrupted.default(legacyInterrupted.default(false))
    )
}
