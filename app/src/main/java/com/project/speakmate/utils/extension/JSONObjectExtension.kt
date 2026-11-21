package com.project.speakmate.utils.extension

import org.json.JSONException
import org.json.JSONObject

fun JSONObject.requireString(key: String): String = get(key) as? String ?: throw JSONException(key)

fun JSONObject.requireBoolean(key: String): Boolean = get(key) as? Boolean ?: throw JSONException(key)

fun JSONObject.requireLong(key: String): Long = when (val value = get(key)) {
    is Long -> value
    is Int -> value.toLong()
    else -> throw JSONException(key)
}
