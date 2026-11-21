package com.project.speakmate.ui.call

import com.project.speakmate.domain.repository.CallRecoveryRepository

class FakeCallRecoveryRepository : CallRecoveryRepository {
    var hasMarker = true
    var failure: Exception? = null
    val users = mutableListOf<String>()

    override suspend fun consumeInterruptedCall(userId: String): Boolean {
        users += userId
        failure?.let { throw it }
        val result = hasMarker
        hasMarker = false
        return result
    }
}
