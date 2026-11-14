package com.project.speakmate.data.repository

import com.project.speakmate.domain.model.DictionaryLookupResult
import com.project.speakmate.domain.model.VocabularyItem
import com.project.speakmate.domain.model.LearningExample
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DictionaryRepositoryImplTest {

    private lateinit var generativeModel: GenerativeModel
    private lateinit var repository: DictionaryRepositoryImpl

    @Before
    fun setUp() {
        generativeModel = mockk()
        repository = DictionaryRepositoryImpl(generativeModel)
    }

    @Test
    fun `lookUp returns Found when Gemini returns valid JSON`() = runTest {
        val validJson = """
            {
              "word": "resilience",
              "mean": "sự kiên cường",
              "phonetic": "/rɪˈzɪl.jəns/",
              "partOfSpeech": "noun",
              "example": {
                "sentence": "She showed great resilience.",
                "translate": "Cô ấy thể hiện sự kiên cường tuyệt vời."
              }
            }
        """.trimIndent()

        val response = mockk<GenerateContentResponse>()
        every { response.text } returns validJson
        coEvery { generativeModel.generateContent(any<com.google.ai.client.generativeai.type.Content>()) } returns response

        val result = repository.lookUp("resilience", "She showed great resilience.")

        assertTrue(result is DictionaryLookupResult.Found)
        val found = result as DictionaryLookupResult.Found
        assertEquals("resilience", found.item.word)
        assertEquals("sự kiên cường", found.item.mean)
        assertEquals("/rɪˈzɪl.jəns/", found.item.phonetic)
        assertEquals("noun", found.item.partOfSpeech)
        assertEquals("She showed great resilience.", found.item.example.sentence)
    }

    @Test
    fun `lookUp returns NotFound when Gemini returns empty text`() = runTest {
        val response = mockk<GenerateContentResponse>()
        every { response.text } returns ""
        coEvery { generativeModel.generateContent(any<com.google.ai.client.generativeai.type.Content>()) } returns response

        val result = repository.lookUp("xyz", "xyz in context")
        assertTrue(result is DictionaryLookupResult.NotFound)
    }

    @Test
    fun `lookUp returns NotFound when Gemini returns null text`() = runTest {
        val response = mockk<GenerateContentResponse>()
        every { response.text } returns null
        coEvery { generativeModel.generateContent(any<com.google.ai.client.generativeai.type.Content>()) } returns response

        val result = repository.lookUp("xyz", "xyz in context")
        assertTrue(result is DictionaryLookupResult.NotFound)
    }

    @Test
    fun `lookUp returns Error when Gemini throws exception`() = runTest {
        coEvery {
            generativeModel.generateContent(any<com.google.ai.client.generativeai.type.Content>())
        } throws RuntimeException("Network error")

        val result = repository.lookUp("test", "test sentence")
        assertTrue(result is DictionaryLookupResult.Error)
        val error = result as DictionaryLookupResult.Error
        assertEquals("Network error", error.throwable.message)
    }

    @Test
    fun `lookUp returns NotFound when JSON has blank word or mean`() = runTest {
        val jsonMissingMean = """{"word":"test","mean":"","phonetic":"","partOfSpeech":"noun"}"""
        val response = mockk<GenerateContentResponse>()
        every { response.text } returns jsonMissingMean
        coEvery { generativeModel.generateContent(any<com.google.ai.client.generativeai.type.Content>()) } returns response

        val result = repository.lookUp("test", "test sentence")
        assertTrue(result is DictionaryLookupResult.NotFound)
    }

    @Test
    fun `lookUp extracts JSON correctly even when wrapped in markdown backticks`() = runTest {
        val markdownWrapped = "```json\n{\"word\":\"run\",\"mean\":\"chạy\",\"phonetic\":\"\",\"partOfSpeech\":\"verb\"}\n```"
        val response = mockk<GenerateContentResponse>()
        every { response.text } returns markdownWrapped
        coEvery { generativeModel.generateContent(any<com.google.ai.client.generativeai.type.Content>()) } returns response

        val result = repository.lookUp("run", "I will run tomorrow.")
        assertTrue(result is DictionaryLookupResult.Found)
        assertEquals("run", (result as DictionaryLookupResult.Found).item.word)
    }
}

