package com.project.speakmate.domain.repository

interface CallRecoveryRepository {
    suspend fun consumeInterruptedCall(userId: String): Boolean
}
