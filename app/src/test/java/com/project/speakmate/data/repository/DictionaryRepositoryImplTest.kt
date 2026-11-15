package com.project.speakmate.data.repository

import com.project.speakmate.domain.model.DictionaryLookupResult
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class DictionaryRepositoryImplTest {

    private fun createRepositoryWithHandler(handler: (String) -> Response): DictionaryRepositoryImpl {
        val interceptor = Interceptor { chain ->
            val url = chain.request().url.toString()
            handler(url)
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
        return DictionaryRepositoryImpl(client)
    }

    private fun makeResponse(url: String, code: Int, json: String): Response {
        val request = okhttp3.Request.Builder().url(url).build()
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("")
            .body(json.toResponseBody("application/json".toMediaType()))
            .build()
    }

    @Test
    fun lookUpReturnsFoundWhenApisReturnValidData() = runTest {
        val freeDictJson = """
            [
              {
                "word": "service",
                "phonetic": "/ˈsɜː.vɪs/",
                "meanings": [
                  {
                    "partOfSpeech": "noun",
                    "definitions": [
                      {
                        "definition": "An act of assistance",
                        "example": "He provided good service."
                      }
                    ]
                  }
                ]
              }
            ]
        """.trimIndent()

        val myMemoryWordJson = """
            {
              "responseData": {
                "translatedText": "dịch vụ"
              }
            }
        """.trimIndent()

        val myMemoryExJson = """
            {
              "responseData": {
                "translatedText": "Anh ấy đã cung cấp dịch vụ tốt."
              }
            }
        """.trimIndent()

        val repository = createRepositoryWithHandler { url ->
            when {
                url.contains("dictionaryapi.dev") -> makeResponse(url, 200, freeDictJson)
                url.contains("He+provided+good+service") || url.contains("He%20provided%20good%20service") -> makeResponse(url, 200, myMemoryExJson)
                url.contains("mymemory") -> makeResponse(url, 200, myMemoryWordJson)
                else -> makeResponse(url, 404, "")
            }
        }

        val result = repository.lookUp("service", "taking a ride-sharing service")
        assertTrue(result is DictionaryLookupResult.Found)
        val found = result as DictionaryLookupResult.Found
        assertEquals("service", found.item.word)
        assertEquals("dịch vụ", found.item.mean)
        assertEquals("/ˈsɜː.vɪs/", found.item.phonetic)
        assertEquals("noun", found.item.partOfSpeech)
        assertEquals("He provided good service.", found.item.example.sentence)
    }

    @Test
    fun lookUpFallsBackToContextSentenceWhenFreeDictHasNoExample() = runTest {
        val freeDictJson = """
            [
              {
                "word": "service",
                "phonetic": "/ˈsɜː.vɪs/",
                "meanings": [
                  {
                    "partOfSpeech": "noun",
                    "definitions": [
                      {
                        "definition": "An act of assistance"
                      }
                    ]
                  }
                ]
              }
            ]
        """.trimIndent()

        val myMemoryWordJson = """{"responseData":{"translatedText":"dịch vụ"}}"""

        val repository = createRepositoryWithHandler { url ->
            when {
                url.contains("dictionaryapi.dev") -> makeResponse(url, 200, freeDictJson)
                url.contains("mymemory") -> makeResponse(url, 200, myMemoryWordJson)
                else -> makeResponse(url, 404, "")
            }
        }

        val context = "taking a ride-sharing service"
        val result = repository.lookUp("service", context)
        assertTrue(result is DictionaryLookupResult.Found)
        val found = result as DictionaryLookupResult.Found
        assertEquals(context, found.item.example.sentence)
    }

    @Test
    fun lookUpReturnsNotFoundWhenBothApisFail() = runTest {
        val repository = createRepositoryWithHandler { url ->
            makeResponse(url, 404, "")
        }

        val result = repository.lookUp("xyzunknown", "")
        assertTrue(result is DictionaryLookupResult.NotFound)
    }

    @Test
    fun lookUpReturnsNotFoundWhenWordIsBlank() = runTest {
        val repository = createRepositoryWithHandler { url ->
            makeResponse(url, 200, "{}")
        }

        val result = repository.lookUp("   ", "")
        assertTrue(result is DictionaryLookupResult.NotFound)
    }

    @Test
    fun lookUpReturnsErrorWhenNetworkThrowsException() = runTest {
        val interceptor = Interceptor {
            throw IOException("Connection failed")
        }
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val repository = DictionaryRepositoryImpl(client)

        val result = repository.lookUp("test", "")
        assertTrue(result is DictionaryLookupResult.Error)
    }
}
