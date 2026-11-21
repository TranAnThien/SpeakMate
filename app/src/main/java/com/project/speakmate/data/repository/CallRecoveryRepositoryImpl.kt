package com.project.speakmate.data.repository

import com.project.speakmate.data.local.ActiveCallLocalDataSource
import com.project.speakmate.domain.repository.CallRecoveryRepository
import javax.inject.Inject

class CallRecoveryRepositoryImpl @Inject constructor(private val markers: ActiveCallLocalDataSource) : CallRecoveryRepository {
    override suspend fun consumeInterruptedCall(userId: String): Boolean = markers.consumeInterruptedCall(userId)
}
