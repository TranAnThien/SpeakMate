package com.project.speakmate.data.session

import com.project.speakmate.domain.model.SenderType
import com.project.speakmate.domain.model.TranscriptMessage
import kotlin.math.roundToInt

class LiveTranscriptAssembler {
    companion object {
        private const val MAX_MESSAGES = 200
        private const val MAX_CONTENT_LENGTH = 2_000
        private const val MESSAGE_ID_PREFIX = "message_"
        private val WHITESPACE = Regex("\\s+")
    }

    private class Entry(val messageId: String, val senderType: SenderType) {
        val text = StringBuilder()
        var isFinal = false
        var isInterrupted = false
    }

    private val entries = mutableListOf<Entry>()
    private var openUser: Entry? = null
    private var openAi: Entry? = null
    private var nextMessageId = 0L
    private var isAiOutputSuppressed = false

    val hasOpenAiTurn: Boolean get() = openAi != null

    val hasOpenUserTurn: Boolean get() = openUser != null

    fun startUserTurn() {
        val current = openUser
        if (current != null && entries.lastOrNull() === current) return
        closeUserTurn()
        openEntry(SenderType.USER, entries.size)
    }

    fun appendUserText(text: String, isFinished: Boolean) {
        if (text.isBlank() && !isFinished) return
        val entry = openUser ?: openUserForLateText() ?: return
        append(entry, text, isFinished)
        if (isFinished) closeUserTurn()
    }

    fun startAiTurn() {
        isAiOutputSuppressed = false
        if (openAi == null) openEntry(SenderType.AI, entries.size)
    }

    fun appendAiText(text: String, isFinished: Boolean) {
        if (isAiOutputSuppressed || text.isBlank() && !isFinished) return
        val entry = openAi ?: openEntry(SenderType.AI, entries.size) ?: return
        append(entry, text, isFinished)
        if (isFinished) closeAiTurn()
    }

    fun completeAiTurn() {
        closeAiTurn()
    }

    fun interruptAiTurn(playedRatio: Float? = null) {
        isAiOutputSuppressed = true
        val entry = openAi ?: return
        entry.isInterrupted = true
        if (playedRatio != null) keepPlayedText(entry, playedRatio)
        closeAiTurn()
    }

    fun interruptOpenTurns(playedRatio: Float? = null) {
        interruptAiTurn(playedRatio)
        closeUserTurn()
    }

    fun snapshot(): List<TranscriptMessage> = entries
        .map { it to normalize(it.text) }
        .filter { (_, content) -> content.isNotEmpty() }
        .mapIndexed { index, (entry, content) ->
            TranscriptMessage(
                messageId = entry.messageId,
                senderType = entry.senderType,
                content = content,
                orderIndex = index,
                isFinal = entry.isFinal,
                isInterrupted = entry.isInterrupted
            )
        }

    private fun openUserForLateText(): Entry? {
        val ai = openAi ?: return openEntry(SenderType.USER, entries.size)
        val aiIndex = entries.indexOf(ai)
        val previous = entries.getOrNull(aiIndex - 1)
        val index = if (aiIndex >= 0 && previous?.senderType != SenderType.USER) aiIndex else entries.size
        return openEntry(SenderType.USER, index)
    }

    private fun openEntry(senderType: SenderType, index: Int): Entry? {
        if (entries.size >= MAX_MESSAGES) return null
        val entry = Entry(MESSAGE_ID_PREFIX + nextMessageId++, senderType)
        entries.add(index.coerceIn(0, entries.size), entry)
        if (senderType == SenderType.USER) openUser = entry else openAi = entry
        return entry
    }

    private fun append(entry: Entry, text: String, isFinished: Boolean) {
        val incoming = normalize(text)
        if (isFinished && incoming.isNotEmpty() && incoming.startsWith(normalize(entry.text))) {
            entry.text.setLength(0)
        }
        entry.text.append(text)
        if (entry.text.length > MAX_CONTENT_LENGTH) entry.text.setLength(MAX_CONTENT_LENGTH)
    }

    private fun keepPlayedText(entry: Entry, playedRatio: Float) {
        if (playedRatio >= 1f) return
        val text = normalize(entry.text)
        val cut = (text.length * playedRatio.coerceAtLeast(0f)).roundToInt()
        val end = if (cut >= text.length || text[cut].isWhitespace()) cut else text.lastIndexOf(' ', cut).coerceAtLeast(0)
        entry.text.setLength(0)
        entry.text.append(text, 0, end.coerceIn(0, text.length))
    }

    private fun closeUserTurn() {
        openUser?.let(::close)
        openUser = null
    }

    private fun closeAiTurn() {
        openAi?.let(::close)
        openAi = null
    }

    private fun close(entry: Entry) {
        entry.isFinal = true
        if (entry.text.isBlank()) entries.remove(entry)
    }

    private fun normalize(text: CharSequence): String = text.trim().replace(WHITESPACE, " ")
}
