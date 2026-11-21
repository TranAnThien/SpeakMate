package com.project.speakmate.data.session

import java.io.IOException

class FakeActiveCallMarker {
    val saved = mutableListOf<String>()
    val removed = mutableListOf<String>()
    var shouldFail = false

    fun save(owner: String) {
        if (shouldFail) throw IOException()
        saved += owner
    }

    fun remove(owner: String) {
        if (shouldFail) throw IOException()
        removed += owner
    }
}
