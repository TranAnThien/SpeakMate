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
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class DictionaryRepositoryImpl
@Inject
constructor(private val client: DictionaryClient, private val responseParser: DictionaryResponseParser) :
    DictionaryRepository {
    companion object {
        private const val DICTIONARY_BASE_URL = "https://api.dictionaryapi.dev/api/v2/entries/en/"
        private const val TRANSLATION_BASE_URL = "https://api.mymemory.translated.net/get?q="
        private const val LANGUAGE_PAIR_QUERY = "&langpair=en|vi"
        private const val CHARSET_NAME = "UTF-8"
        private const val LOOKUP_FAILED = "Dictionary lookup failed"
    }

    override suspend fun lookUp(word: String, sentence: String): DictionaryLookupResult =
        withContext(Dispatchers.IO) {
            val cleanWord = word.trim().lowercase(Locale.ROOT)
            if (cleanWord.isBlank()) {
                return@withContext DictionaryLookupResult.NotFound
            }

            try {
                val encodedWord = URLEncoder.encode(cleanWord, CHARSET_NAME)
                val freeDictUrl = "$DICTIONARY_BASE_URL$encodedWord"
                val myMemoryWordUrl = "$TRANSLATION_BASE_URL$encodedWord$LANGUAGE_PAIR_QUERY"

                val (dictJson, myMemoryJson) =
                    coroutineScope {
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

                if (exampleSentence.isBlank() &&
                    sentence.isNotBlank() &&
                    sentence.contains(cleanWord, ignoreCase = true)) {
                    exampleSentence = sentence.trim()
                }

                var exampleTranslate = ""
                if (exampleSentence.isNotBlank()) {
                    try {
                        val encodedEx = URLEncoder.encode(exampleSentence, CHARSET_NAME)
                        val myMemoryExUrl = "$TRANSLATION_BASE_URL$encodedEx$LANGUAGE_PAIR_QUERY"
                        val exJson = client.fetchJson(myMemoryExUrl)
                        exampleTranslate = responseParser.readExampleTranslation(exJson)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {}
                }

                val item =
                    VocabularyItem(
                        word = cleanWord,
                        mean = meaning,
                        phonetic = phonetic,
                        partOfSpeech = partOfSpeech,
                        example = LearningExample(sentence = exampleSentence, translate = exampleTranslate)
                    )

                DictionaryLookupResult.Found(item)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                Log.e(AppConstant.DICTIONARY_LOG_TAG, LOOKUP_FAILED)
                DictionaryLookupResult.Error(e)
            }
        }
}
