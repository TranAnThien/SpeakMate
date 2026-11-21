package com.project.speakmate.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import io.mockk.every
import io.mockk.mockk

object TrainingContentTestFixtures {
    fun getDocument(fields: Map<String, Any?>): DocumentSnapshot {
        val document = mockk<DocumentSnapshot>()
        every { document.id } returns "document-id"
        every { document.getString(any()) } answers { fields[firstArg<String>()] as? String }
        every { document.get(any<String>()) } answers { fields[firstArg<String>()] }
        return document
    }

    fun getExample(sentence: String = "Example", translation: String = "Translation"): Map<String, String> =
        mapOf("sentence" to sentence, "translate" to translation)

    fun getVocabulary(): Map<String, Any?> = mapOf(
        "word" to "coffee", "mean" to "drink", "phonetic" to "/coffee/", "partOfSpeech" to "noun",
        "example" to getExample()
    )

    fun getGrammar(): Map<String, Any?> = mapOf(
        "title" to "Present simple", "structure" to "I work", "explanation" to "Routine", "usage" to "Every day",
        "examples" to listOf(getExample("First", "One"), getExample("Second", "Two"))
    )

    fun getPattern(): Map<String, Any?> = mapOf(
        "pattern" to "May I...", "mean" to "Request", "usage" to "Polite request",
        "examples" to listOf(getExample("First", "One"), getExample("Second", "Two"))
    )
}
