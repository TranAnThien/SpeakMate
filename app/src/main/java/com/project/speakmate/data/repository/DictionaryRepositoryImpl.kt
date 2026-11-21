package com.project.speakmate.data.repository

import android.util.Log
import com.project.speakmate.data.remote.dictionary.DictionaryClient
import com.project.speakmate.data.remote.dictionary.DictionaryResponseParser
import com.project.speakmate.domain.model.DictionaryLookupResult
import com.project.speakmate.domain.model.LearningExample
import com.project.speakmate.domain.model.VocabularyItem
import com.project.speakmate.domain.repository.DictionaryRepository
import com.project.speakmate.utils.AppConstant
import java.net.URLEncoder
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

class DictionaryRepositoryImpl @Inject constructor(
    httpClient: OkHttpClient
) : DictionaryRepository {

    private val client = DictionaryClient(httpClient)
    private val responseParser = DictionaryResponseParser()

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
                val dictDeferred = async { client.fetchJson(freeDictUrl) }
                val myMemoryDeferred = async { client.fetchJson(myMemoryWordUrl) }
                Pair(dictDeferred.await(), myMemoryDeferred.await())
            }

            val entry = responseParser.readEntry(dictJson)
            val phonetic = entry.phonetic
            val partOfSpeech = entry.partOfSpeech
            var exampleSentence = entry.exampleSentence

            var meaning = responseParser.readMeaning(myMemoryJson)

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
                    val exJson = client.fetchJson(myMemoryExUrl)
                    exampleTranslate = responseParser.readExampleTranslation(exJson)
                } catch (cancelled: CancellationException) {
                    throw cancelled
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
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            Log.e(AppConstant.DICTIONARY_LOG_TAG, "Dictionary lookup failed")
            DictionaryLookupResult.Error(e)
        }
    }

}
