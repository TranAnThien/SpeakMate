package com.project.speakmate.test

import com.project.speakmate.domain.model.SpeakingCriteriaResult
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingProgress
import com.project.speakmate.domain.model.User
import com.project.speakmate.domain.repository.UserRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class FakeUserRepository(user: User? = null) : UserRepository {
    private val profiles = mutableMapOf<String, MutableStateFlow<User?>>()
    var shouldFailObservation = false
    var observationStarts = 0
    var activeObservers = 0
    var saveFailure: Exception? = null
    var saveGate: CompletableDeferred<Unit>? = null
    var saveCalls = 0
    var skipCalls = 0
    var savedLevel: SpeakingLevel? = null
    var savedCriteria: SpeakingCriteriaResult? = null
    var savedAtMillis: Long? = null
    var savedProgress: TrainingProgress? = null

    init {
        if (user != null) setUser(user)
    }

    override suspend fun getUser(userId: String): Result<User?> = Result.success(profile(userId).value)

    override suspend fun getOrCreateUser(user: User): Result<User> {
        setUser(user)
        return Result.success(user)
    }

    override fun observeUser(userId: String): Flow<User?> = flow {
        observationStarts++
        activeObservers++
        try {
            if (shouldFailObservation) throw IllegalStateException()
            emitAll(profile(userId))
        } finally {
            activeObservers--
        }
    }

    override suspend fun saveAssessedLevel(
        level: SpeakingLevel,
        criteria: SpeakingCriteriaResult,
        assessedAtMillis: Long,
        trainingProgress: TrainingProgress
    ): Result<Unit> {
        saveCalls++
        saveGate?.await()
        val failure = saveFailure
        if (failure != null) return Result.failure(failure)
        savedLevel = level
        savedCriteria = criteria
        savedAtMillis = assessedAtMillis
        savedProgress = trainingProgress
        return Result.success(Unit)
    }

    override suspend fun markNotAssessed(): Result<Unit> {
        skipCalls++
        val failure = saveFailure
        return if (failure == null) Result.success(Unit) else Result.failure(failure)
    }

    fun setUser(user: User) {
        profile(user.userId).value = user
    }

    private fun profile(userId: String): MutableStateFlow<User?> =
        profiles.getOrPut(userId) { MutableStateFlow(null) }
}
