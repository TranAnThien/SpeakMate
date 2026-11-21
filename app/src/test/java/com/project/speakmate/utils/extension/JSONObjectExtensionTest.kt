package com.project.speakmate.utils.extension

import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class JSONObjectExtensionTest {
    @Test
    fun readsValuesWithTheExpectedTypes() {
        val json = JSONObject().put("label", "text").put("enabled", false)
            .put("count", 2).put("time", Long.MAX_VALUE)

        assertEquals("text", json.requireString("label"))
        assertEquals(false, json.requireBoolean("enabled"))
        assertEquals(2L, json.requireLong("count"))
        assertEquals(Long.MAX_VALUE, json.requireLong("time"))
    }

    @Test
    fun rejectsCoercionsAndMissingFields() {
        val json = JSONObject().put("flag", "true").put("fraction", 2.5)
            .put("number", 2).put("empty", JSONObject.NULL)

        assertThrows(JSONException::class.java) { json.requireBoolean("flag") }
        assertThrows(JSONException::class.java) { json.requireLong("fraction") }
        assertThrows(JSONException::class.java) { json.requireString("number") }
        assertThrows(JSONException::class.java) { json.requireLong("empty") }
        assertThrows(JSONException::class.java) { json.requireLong("missing") }
    }
}
