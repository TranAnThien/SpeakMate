package com.project.speakmate.data.repository

import android.util.Log
import com.project.speakmate.domain.model.DictionaryLookupResult
import com.project.speakmate.domain.model.LearningExample
import com.project.speakmate.domain.model.VocabularyItem
import com.project.speakmate.domain.repository.DictionaryRepository
import java.net.URLEncoder
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class DictionaryRepositoryImpl @Inject constructor(
    private val httpClient: OkHttpClient
) : DictionaryRepository {

    override suspend fun lookUp(word: String, sentence: String): DictionaryLookupResult = withContext(Dispatchers.IO) {
        val cleanWord = word.trim().lowercase()
        if (cleanWord.isBlank()) {
            return@withContext DictionaryLookupResult.NotFound
        }

        try {
            val encodedWord = URLEncoder.encode(cleanWord, "UTF-8")
            val freeDictUrl = "https://api.dictionaryapi.dev/api/v2/entries/en/$encodedWord"
            val myMemoryWordUrl = "https://api.mymemory.translated.net/get?q=$encodedWord&langpair=en|vi"

            val (dictJson, myMemoryJson) = coroutineScope {
                val dictDeferred = async { fetchJson(freeDictUrl) }
                val myMemoryDeferred = async { fetchJson(myMemoryWordUrl) }
                Pair(dictDeferred.await(), myMemoryDeferred.await())
            }

            var phonetic = ""
            var partOfSpeech = ""
            var exampleSentence = ""

            if (dictJson != null) {
                try {
                    val rootArray = JSONArray(dictJson)
                    if (rootArray.length() > 0) {
                        val firstEntry = rootArray.getJSONObject(0)
                        phonetic = firstEntry.optString("phonetic", "")
                        if (phonetic.isBlank()) {
                            val phoneticsArr = firstEntry.optJSONArray("phonetics")
                            if (phoneticsArr != null) {
                                for (i in 0 until phoneticsArr.length()) {
                                    val p = phoneticsArr.optJSONObject(i)?.optString("text", "") ?: ""
                                    if (p.isNotBlank()) {
                                        phonetic = p
                                        break
                                    }
                                }
                            }
                        }

                        val meaningsArr = firstEntry.optJSONArray("meanings")
                        if (meaningsArr != null && meaningsArr.length() > 0) {
                            val firstMeaning = meaningsArr.optJSONObject(0)
                            partOfSpeech = firstMeaning?.optString("partOfSpeech", "") ?: ""
                            val defs = firstMeaning?.optJSONArray("definitions")
                            if (defs != null) {
                                for (i in 0 until defs.length()) {
                                    val ex = defs.optJSONObject(i)?.optString("example", "") ?: ""
                                    if (ex.isNotBlank()) {
                                        exampleSentence = ex
                                        break
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("DictionaryRepository", "Failed to parse Free Dictionary response: ${e.message}")
                }
            }

            var meaning = ""
            if (myMemoryJson != null) {
                try {
                    val rootObj = JSONObject(myMemoryJson)
                    val trans = rootObj.optJSONObject("responseData")?.optString("translatedText", "") ?: ""
                    if (trans.isNotBlank() && !trans.startsWith("MYMEMORY WARNING")) {
                        meaning = cleanHtmlEntities(trans)
                    }
                } catch (e: Exception) {
                    Log.w("DictionaryRepository", "Failed to parse MyMemory response: ${e.message}")
                }
            }

            if (meaning.isBlank() && dictJson == null) {
                return@withContext DictionaryLookupResult.NotFound
            }

            if (meaning.isBlank()) {
                meaning = cleanWord
            }

            if (exampleSentence.isBlank() && sentence.isNotBlank() && sentence.contains(cleanWord, ignoreCase = true)) {
                exampleSentence = sentence.trim()
            }

            var exampleTranslate = ""
            if (exampleSentence.isNotBlank()) {
                try {
                    val encodedEx = URLEncoder.encode(exampleSentence, "UTF-8")
                    val myMemoryExUrl = "https://api.mymemory.translated.net/get?q=$encodedEx&langpair=en|vi"
                    val exJson = fetchJson(myMemoryExUrl)
                    if (exJson != null) {
                        val rootEx = JSONObject(exJson)
                        val exTrans = rootEx.optJSONObject("responseData")?.optString("translatedText", "") ?: ""
                        if (exTrans.isNotBlank() && !exTrans.startsWith("MYMEMORY WARNING")) {
                            exampleTranslate = cleanHtmlEntities(exTrans)
                        }
                    }
                } catch (_: Exception) {}
            }

            val item = VocabularyItem(
                word = cleanWord,
                mean = meaning,
                phonetic = phonetic,
                partOfSpeech = partOfSpeech,
                example = LearningExample(
                    sentence = exampleSentence,
                    translate = exampleTranslate
                )
            )

            DictionaryLookupResult.Found(item)
        } catch (e: Exception) {
            Log.e("DictionaryRepository", "Error looking up word '$cleanWord': ${e.message}", e)
            DictionaryLookupResult.Error(e)
        }
    }

    private fun fetchJson(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "SpeakMate-App")
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            return response.body?.string()
        }
    }

    private fun cleanHtmlEntities(text: String): String {
        return text.replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .trim()
    }
}
