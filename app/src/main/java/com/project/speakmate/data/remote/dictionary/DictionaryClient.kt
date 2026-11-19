package com.project.speakmate.data.remote.dictionary

import okhttp3.OkHttpClient
import okhttp3.Request

class DictionaryClient(private val httpClient: OkHttpClient) {
    fun fetchJson(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "SpeakMate-App")
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            return response.body?.string()
        }
    }

}
