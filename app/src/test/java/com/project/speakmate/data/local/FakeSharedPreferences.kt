package com.project.speakmate.data.local

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import java.io.IOException

class FakeSharedPreferences {
    val values = mutableMapOf<String, String>()
    val preferences = mockk<SharedPreferences>()
    var shouldFail = false
    var operations = 0
    private val editor = mockk<SharedPreferences.Editor>()
    private val pending = mutableMapOf<String, String?>()

    init {
        every { preferences.getString(any(), any()) } answers {
            checkStorage()
            values[firstArg<String>()]
        }
        every { preferences.all } answers { checkStorage(); values.toMap() }
        every { preferences.edit() } answers { checkStorage(); editor }
        every { editor.putString(any(), any()) } answers { pending[firstArg()] = secondArg(); editor }
        every { editor.remove(any()) } answers { pending[firstArg()] = null; editor }
        every { editor.commit() } answers {
            checkStorage()
            pending.forEach { (key, value) -> if (value == null) values.remove(key) else values[key] = value }
            pending.clear()
            true
        }
    }

    private fun checkStorage() {
        operations++
        if (shouldFail) throw IOException()
    }
}
