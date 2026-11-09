package com.project.speakmate.data.session

import com.project.speakmate.domain.model.SenderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveTranscriptAssemblerTest {
    companion object {
        private const val MAX_MESSAGES = 200
        private const val MAX_CONTENT_LENGTH = 2_000
    }

    private val assembler = LiveTranscriptAssembler()

    @Test
    fun keepsTurnsInConversationOrder() {
        assembler.startUserTurn()
        assembler.appendUserText("Hi", false)
        assembler.startAiTurn()
        assembler.appendAiText("Hello!", false)
        assembler.completeAiTurn()
        assembler.startUserTurn()
        assembler.appendUserText("A latte", false)

        val messages = assembler.snapshot()

        assertEquals(listOf(SenderType.USER, SenderType.AI, SenderType.USER), messages.map { it.senderType })
        assertEquals(listOf(0, 1, 2), messages.map { it.orderIndex })
        assertEquals(listOf("Hi", "Hello!", "A latte"), messages.map { it.content })
    }

    @Test
    fun lateUserTextStaysBeforeTheAnswerItTriggered() {
        assembler.startUserTurn()
        assembler.startAiTurn()
        assembler.appendAiText("Sure, what size?", false)
        assembler.appendUserText("I want a coffee", false)

        val messages = assembler.snapshot()

        assertEquals("I want a coffee", messages[0].content)
        assertEquals(SenderType.AI, messages[1].senderType)
    }

    @Test
    fun lateUserTextWithoutVoiceActivityIsInsertedBeforeOpenAnswer() {
        assembler.startAiTurn()
        assembler.appendAiText("Welcome", false)
        assembler.completeAiTurn()
        assembler.startAiTurn()
        assembler.appendAiText("Anything else?", false)
        assembler.appendUserText("Yes", false)

        assertEquals(listOf("Welcome", "Yes", "Anything else?"), assembler.snapshot().map { it.content })
    }

    @Test
    fun partialChunksJoinAndStayInOneMessage() {
        assembler.startUserTurn()
        assembler.appendUserText("I'd like", false)
        assembler.appendUserText(" a  latte", false)

        val message = assembler.snapshot().single()

        assertEquals("I'd like a latte", message.content)
        assertFalse(message.isFinal)
    }

    @Test
    fun finalTextReplacesMatchingPartial() {
        assembler.startUserTurn()
        assembler.appendUserText("I'd like a", false)
        assembler.appendUserText("I'd like a latte.", true)

        val message = assembler.snapshot().single()

        assertEquals("I'd like a latte.", message.content)
        assertTrue(message.isFinal)
    }

    @Test
    fun finalDeltaIsAppended() {
        assembler.startAiTurn()
        assembler.appendAiText("Small or", false)
        assembler.appendAiText(" large?", true)

        assertEquals("Small or large?", assembler.snapshot().single().content)
    }

    @Test
    fun finishedUserTurnIsLocked() {
        assembler.startUserTurn()
        assembler.appendUserText("Hello", true)
        assembler.appendUserText("Again", false)

        val messages = assembler.snapshot()

        assertEquals(listOf("Hello", "Again"), messages.map { it.content })
        assertTrue(messages[0].isFinal)
        assertEquals(messages.map { it.messageId }.distinct().size, messages.size)
    }

    @Test
    fun repeatedUserStartBeforeAnswerKeepsOneTurn() {
        assembler.startUserTurn()
        assembler.appendUserText("One", false)
        assembler.startUserTurn()
        assembler.appendUserText(" two", false)

        assertEquals("One two", assembler.snapshot().single().content)
    }

    @Test
    fun blankChunksAndSilentTurnsAreDropped() {
        assembler.startUserTurn()
        assembler.appendUserText("   ", false)
        assembler.startAiTurn()
        assembler.completeAiTurn()

        assertTrue(assembler.snapshot().isEmpty())
    }

    @Test
    fun messageIdStaysStableWhileTextGrows() {
        assembler.startAiTurn()
        assembler.appendAiText("Hi", false)
        val firstId = assembler.snapshot().single().messageId
        assembler.appendAiText(" there", false)

        assertEquals(firstId, assembler.snapshot().single().messageId)
    }

    @Test
    fun interruptedAnswerIsFinalAndMarked() {
        assembler.startAiTurn()
        assembler.appendAiText("Our special today is", false)
        assembler.interruptAiTurn()
        assembler.startAiTurn()
        assembler.appendAiText("New answer", false)

        val messages = assembler.snapshot()

        assertTrue(messages[0].isInterrupted)
        assertTrue(messages[0].isFinal)
        assertFalse(messages[1].isInterrupted)
    }

    @Test
    fun interruptedAnswerKeepsOnlyPlayedWords() {
        assembler.startAiTurn()
        assembler.appendAiText("Our special today is pumpkin latte", false)
        assembler.interruptAiTurn(0.5f)

        assertEquals("Our special today", assembler.snapshot().single().content)
    }

    @Test
    fun interruptedAnswerCutsBackToWordBoundary() {
        assembler.startAiTurn()
        assembler.appendAiText("Our special today", false)
        assembler.interruptAiTurn(0.5f)

        assertEquals("Our", assembler.snapshot().single().content)
    }

    @Test
    fun answerInterruptedBeforeAnyAudioIsDropped() {
        assembler.startAiTurn()
        assembler.appendAiText("Welcome to", false)
        assembler.interruptAiTurn(0f)

        assertTrue(assembler.snapshot().isEmpty())
    }

    @Test
    fun fullyPlayedInterruptedAnswerKeepsText() {
        assembler.startAiTurn()
        assembler.appendAiText("Here you go", false)
        assembler.interruptAiTurn(1f)

        assertEquals("Here you go", assembler.snapshot().single().content)
    }

    @Test
    fun lateOutputAfterInterruptionWaitsForNextModelTurn() {
        assembler.startAiTurn()
        assembler.appendAiText("Our special", false)
        assembler.interruptAiTurn(1f)
        assembler.appendAiText(" today is", false)

        assertEquals(listOf("Our special"), assembler.snapshot().map { it.content })

        assembler.startAiTurn()
        assembler.appendAiText("Sure", false)

        assertEquals(listOf("Our special", "Sure"), assembler.snapshot().map { it.content })
    }

    @Test
    fun reportsOpenAiTurn() {
        assertFalse(assembler.hasOpenAiTurn)
        assembler.startAiTurn()
        assertTrue(assembler.hasOpenAiTurn)
        assembler.completeAiTurn()
        assertFalse(assembler.hasOpenAiTurn)
    }

    @Test
    fun interruptingOpenTurnsFinalizesEverything() {
        assembler.startUserTurn()
        assembler.appendUserText("Hello", false)
        assembler.startAiTurn()
        assembler.appendAiText("Hi", false)
        assembler.interruptOpenTurns()

        val messages = assembler.snapshot()

        assertTrue(messages.all { it.isFinal })
        assertFalse(messages[0].isInterrupted)
        assertTrue(messages[1].isInterrupted)
    }

    @Test
    fun capsMessageCountAndLength() {
        repeat(MAX_MESSAGES + 5) {
            assembler.appendAiText("x", true)
        }
        assembler.snapshot().let { assertEquals(MAX_MESSAGES, it.size) }

        val single = LiveTranscriptAssembler()
        single.appendUserText("a".repeat(MAX_CONTENT_LENGTH + 10), false)

        assertEquals(MAX_CONTENT_LENGTH, single.snapshot().single().content.length)
    }
}
