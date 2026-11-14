package com.project.speakmate.data.repository

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.project.speakmate.BuildConfig
import com.project.speakmate.domain.model.DictionaryLookupResult
import com.project.speakmate.domain.model.LearningExample
import com.project.speakmate.domain.model.VocabularyItem
import com.project.speakmate.domain.repository.DictionaryRepository
import org.json.JSONObject
import javax.inject.Inject

class DictionaryRepositoryImpl @Inject constructor(
    private val generativeModel: GenerativeModel
) : DictionaryRepository {

    override suspend fun lookUp(word: String, sentenceContext: String): DictionaryLookupResult {
        if (BuildConfig.GEMINI_API_KEY.isBlank()) {
            return DictionaryLookupResult.Found(
                VocabularyItem(
                    word = word.trim().lowercase(),
                    mean = "[Mock] Chưa cấu hình API key",
                    phonetic = "",
                    partOfSpeech = "",
                    example = LearningExample(sentence = "", translate = "")
                )
            )
        }

        return try {
            val prompt = """
                Bạn là một từ điển tiếng Anh cho người Việt. Hãy giải thích từ "$word" dựa trên ngữ cảnh của câu: "$sentenceContext".
                Trả về DUY NHẤT một chuỗi JSON hợp lệ (không chứa markdown), bao gồm các field chính xác như sau:
                {
                  "word": "từ tiếng Anh đã chuẩn hóa lowercase",
                  "mean": "nghĩa tiếng Việt ngắn gọn theo ngữ cảnh",
                  "phonetic": "phiên âm quốc tế IPA",
                  "partOfSpeech": "loại từ (vd: noun, verb, adj)",
                  "example": {
                    "sentence": "1 câu ví dụ tiếng Anh ngắn có chứa từ này",
                    "translate": "nghĩa tiếng Việt của câu ví dụ"
                  }
                }
                Nếu không tìm thấy hoặc không phải từ hợp lệ, hãy trả về chuỗi rỗng.
            """.trimIndent()

            val response = generativeModel.generateContent(
                content { text(prompt) }
            )

            val rawText = response.text?.trim() ?: return DictionaryLookupResult.NotFound

            // Trích xuất JSON object đầu tiên từ chuỗi trả về (đề phòng Gemini bọc markdown)
            val start = rawText.indexOf('{')
            val end = rawText.lastIndexOf('}')
            if (start == -1 || end == -1 || start >= end) {
                return DictionaryLookupResult.NotFound
            }
            val jsonText = rawText.substring(start, end + 1)

            val jsonObject = JSONObject(jsonText)
            val wordValue = jsonObject.optString("word", "").trim()
            val meanValue = jsonObject.optString("mean", "").trim()

            if (wordValue.isBlank() || meanValue.isBlank()) {
                return DictionaryLookupResult.NotFound
            }

            val exampleObj = jsonObject.optJSONObject("example")
            val item = VocabularyItem(
                word = wordValue,
                mean = meanValue,
                phonetic = jsonObject.optString("phonetic", ""),
                partOfSpeech = jsonObject.optString("partOfSpeech", ""),
                example = LearningExample(
                    sentence = exampleObj?.optString("sentence", "") ?: "",
                    translate = exampleObj?.optString("translate", "") ?: ""
                )
            )

            DictionaryLookupResult.Found(item)
        } catch (e: Exception) {
            DictionaryLookupResult.Error(e)
        }
    }
}