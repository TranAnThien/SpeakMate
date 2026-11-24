package com.project.speakmate.data.remote.dictionary

import javax.inject.Inject
import okhttp3.OkHttpClient
import okhttp3.Request

class DictionaryClient @Inject constructor(private val httpClient: OkHttpClient) {
    companion object {
        private const val HEADER_USER_AGENT = "User-Agent"
        private const val USER_AGENT_VALUE = "SpeakMate-App"
    }

    fun fetchJson(url: String): String? {
        val request = Request.Builder().url(url).header(HEADER_USER_AGENT, USER_AGENT_VALUE).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            return response.body?.string()
        }
    }
}
