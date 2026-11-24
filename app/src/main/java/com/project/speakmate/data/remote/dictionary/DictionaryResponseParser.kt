package com.project.speakmate.data.remote.dictionary

import android.util.Log
import com.project.speakmate.utils.AppConstant
import com.project.speakmate.utils.extension.defaultEmpty
import javax.inject.Inject
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class DictionaryResponseParser @Inject constructor() {
    fun readEntry(dictJson: String?): DictionaryEntryResponse {
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
                                val p = phoneticsArr.optJSONObject(i)?.optString("text", "").defaultEmpty()
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
                        partOfSpeech = firstMeaning?.optString("partOfSpeech", "").defaultEmpty()
                        val defs = firstMeaning?.optJSONArray("definitions")
                        if (defs != null) {
                            for (i in 0 until defs.length()) {
                                val ex = defs.optJSONObject(i)?.optString("example", "").defaultEmpty()
                                if (ex.isNotBlank()) {
                                    exampleSentence = ex
                                    break
                                }
                            }
                        }
                    }
                }
            } catch (_: JSONException) {
                Log.w(AppConstant.DICTIONARY_LOG_TAG, "Failed to parse Free Dictionary response")
            }
        }
        return DictionaryEntryResponse(
            phonetic = phonetic,
            partOfSpeech = partOfSpeech,
            exampleSentence = exampleSentence
        )
    }

    fun readMeaning(myMemoryJson: String?): String {
        var meaning = ""
        if (myMemoryJson != null) {
            try {
                val rootObj = JSONObject(myMemoryJson)
                val trans = rootObj.optJSONObject("responseData")?.optString("translatedText", "").defaultEmpty()
                if (trans.isNotBlank() && !trans.startsWith("MYMEMORY WARNING")) {
                    meaning = cleanHtmlEntities(trans)
                }
            } catch (_: JSONException) {
                Log.w(AppConstant.DICTIONARY_LOG_TAG, "Failed to parse MyMemory response")
            }
        }
        return meaning
    }

    fun readExampleTranslation(exJson: String?): String {
        var exampleTranslate = ""
        if (exJson != null) {
            val rootEx = JSONObject(exJson)
            val exTrans = rootEx.optJSONObject("responseData")?.optString("translatedText", "").defaultEmpty()
            if (exTrans.isNotBlank() && !exTrans.startsWith("MYMEMORY WARNING")) {
                exampleTranslate = cleanHtmlEntities(exTrans)
            }
        }
        return exampleTranslate
    }

    private fun cleanHtmlEntities(text: String): String {
        return text
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .trim()
    }
}
